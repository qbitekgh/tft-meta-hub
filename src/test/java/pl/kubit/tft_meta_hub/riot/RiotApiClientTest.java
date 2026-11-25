package pl.kubit.tft_meta_hub.riot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Failure;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.KeyInvalid;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.NotFound;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.RateLimited;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Success;
import pl.kubit.tft_meta_hub.riot.RiotProperties.RateLimit;
import pl.kubit.tft_meta_hub.riot.dto.LeagueListDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.ParticipantDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.TraitDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto.UnitDto;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The client against a fake Riot server ({@link MockRestServiceServer}) returning anonymised real responses
 * from {@code src/test/resources/riot}. No request leaves the test.
 */
class RiotApiClientTest {

	private static final String TEST_KEY = "RGAPI-test-key";

	private MockRestServiceServer server;
	private MutableClock clock;
	private RiotApiClient client;

	@BeforeEach
	void setUp() {
		RiotProperties properties = properties(TEST_KEY);
		RestClient.Builder builder = RestClient.builder().defaultHeader("X-Riot-Token", TEST_KEY);
		server = MockRestServiceServer.bindTo(builder).build();
		clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
		RiotRateLimiter limiter = new RiotRateLimiter(properties.rateLimits(), clock, clock);
		client = new RiotApiClient(builder.build(), limiter, properties);
	}

	@Test
	void shouldCallPlatformHostWithKeyHeader_whenReadingLadder() {
		server.expect(requestTo("https://eun1.api.test/tft/league/v1/challenger?queue=RANKED_TFT"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Riot-Token", TEST_KEY))
				.andRespond(withSuccess(fixture("league-challenger.json"), MediaType.APPLICATION_JSON));

		RiotApiResult<LeagueListDto> result = client.getLeague(Platform.EUNE, LeagueTier.CHALLENGER);

		server.verify();
		LeagueListDto ladder = successBody(result);
		assertThat(ladder.tier()).isEqualTo("CHALLENGER");
		assertThat(ladder.entries()).extracting(LeagueListDto.LeagueEntryDto::puuid)
				.containsExactly("test-puuid-1", "test-puuid-2", "test-puuid-3");
	}

	@Test
	void shouldCallRegionalHostWithStartTimeInSeconds_whenReadingMatchIds() {
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/by-puuid/test-puuid-1/ids?count=20&startTime=1795000000"))
				.andRespond(withSuccess(fixture("match-ids.json"), MediaType.APPLICATION_JSON));

		RiotApiResult<List<String>> result = client.getMatchIds("test-puuid-1", Instant.ofEpochSecond(1_795_000_000), 20);

		assertThat(successBody(result)).hasSize(5).allMatch(id -> id.startsWith("EUN1_"));
	}

	@Test
	void shouldOmitStartTime_whenPlayerWasNeverCollected() {
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/by-puuid/test-puuid-1/ids?count=20"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertThat(successBody(client.getMatchIds("test-puuid-1", null, 20))).isEmpty();
	}

	@Test
	void shouldMapPlacementsUnitsItemsAndTraits_whenReadingMatch() {
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_4027724406"))
				.andRespond(withSuccess(fixture("match.json"), MediaType.APPLICATION_JSON));

		MatchDto match = successBody(client.getMatch("EUN1_4027724406"));

		assertThat(match.metadata().matchId()).isEqualTo("EUN1_4027724406");
		assertThat(match.info().queueId()).isEqualTo(1100);
		assertThat(match.info().tftSetNumber()).isEqualTo(18);
		assertThat(match.info().gameDatetime()).isPositive();
		assertThat(match.info().participants()).extracting(ParticipantDto::placement, ParticipantDto::level)
				.containsExactly(tuple(1, 9), tuple(8, 6));
		ParticipantDto winner = match.info().participants().getFirst();
		assertThat(winner.units()).extracting(UnitDto::characterId).contains("DA_18_Rakan");
		assertThat(winner.units()).allSatisfy(unit -> assertThat(unit.starLevel()).isBetween(1, 3));
		assertThat(winner.units()).flatExtracting(UnitDto::itemNames).allMatch(item -> item.startsWith("DA_"));
		assertThat(winner.traits().stream().filter(TraitDto::isActive)).hasSize(10);
	}

	@Test
	void shouldReturnNotFound_whenMatchDoesNotExist() {
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_1"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).body(fixture("not-found.json")).contentType(MediaType.TEXT_PLAIN));

		assertThat(client.getMatch("EUN1_1")).isInstanceOf(NotFound.class);
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 403})
	void shouldReturnKeyInvalidWithoutRetrying_whenKeyIsRejected(int status) {
		server.expect(times(1), requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_1"))
				.andRespond(withStatus(HttpStatus.valueOf(status)));

		RiotApiResult<MatchDto> result = client.getMatch("EUN1_1");

		server.verify();
		assertThat(result).isEqualTo(new KeyInvalid<MatchDto>(status));
	}

	@Test
	void shouldReturnKeyInvalidWithoutAnyRequest_whenKeyIsNotConfigured() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(builder).build();
		RiotProperties noKey = properties("");
		RiotApiClient clientWithoutKey = new RiotApiClient(builder.build(),
				new RiotRateLimiter(noKey.rateLimits(), clock, clock), noKey);

		assertThat(clientWithoutKey.getMatch("EUN1_1")).isInstanceOf(KeyInvalid.class);
		emptyServer.verify();
	}

	@Test
	void shouldWaitRetryAfterAndRetry_whenRiotAnswers429Once() {
		HttpHeaders retryAfter = new HttpHeaders();
		retryAfter.add("Retry-After", "5");
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_4027724406"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_4027724406"))
				.andRespond(withSuccess(fixture("match.json"), MediaType.APPLICATION_JSON));

		RiotApiResult<MatchDto> result = client.getMatch("EUN1_4027724406");

		server.verify();
		assertThat(result).isInstanceOf(Success.class);
		assertThat(clock.totalSlept()).isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	void shouldGiveUpAfterMaxAttempts_whenRiotKeepsAnswering429() {
		HttpHeaders retryAfter = new HttpHeaders();
		retryAfter.add("Retry-After", "2");
		server.expect(times(3), requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_1"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));

		RiotApiResult<MatchDto> result = client.getMatch("EUN1_1");

		server.verify();
		assertThat(result).isEqualTo(new RateLimited<MatchDto>(Duration.ofSeconds(2)));
	}

	@Test
	void shouldReturnFailure_whenServerFails() {
		server.expect(requestTo("https://europe.api.test/tft/match/v1/matches/EUN1_1"))
				.andRespond(withServerError());

		assertThat(client.getMatch("EUN1_1")).isEqualTo(new Failure<MatchDto>("HTTP 500"));
	}

	@Test
	void shouldUseOneSecond_whenRetryAfterIsMissingOrInvalid() {
		assertThat(RiotApiClient.parseRetryAfter(null)).isEqualTo(Duration.ofSeconds(1));
		assertThat(RiotApiClient.parseRetryAfter("soon")).isEqualTo(Duration.ofSeconds(1));
		assertThat(RiotApiClient.parseRetryAfter(" 12 ")).isEqualTo(Duration.ofSeconds(12));
	}

	/** Unwraps a successful result with a record pattern; any other outcome fails the test with a clear message. */
	private static <T> T successBody(RiotApiResult<T> result) {
		return switch (result) {
			case Success<T>(T body) -> body;
			case NotFound<T> notFound -> throw new AssertionError("Expected success but got 404");
			case KeyInvalid<T>(int status) -> throw new AssertionError("Expected success but key was rejected: " + status);
			case RateLimited<T>(Duration wait) -> throw new AssertionError("Expected success but was rate limited");
			case Failure<T>(String message) -> throw new AssertionError("Expected success but failed: " + message);
		};
	}

	private static RiotProperties properties(String apiKey) {
		return new RiotProperties(apiKey, "https://{host}.api.test", "europe",
				List.of(new RateLimit(20, Duration.ofSeconds(1)), new RateLimit(100, Duration.ofMinutes(2))), 3);
	}

	private static String fixture(String name) {
		try (InputStream input = RiotApiClientTest.class.getResourceAsStream("/riot/" + name)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
