package pl.kubit.tft_meta_hub.collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.gamedata.PatchCalendar;
import pl.kubit.tft_meta_hub.riot.LeagueTier;
import pl.kubit.tft_meta_hub.riot.MutableClock;
import pl.kubit.tft_meta_hub.riot.Platform;
import pl.kubit.tft_meta_hub.riot.RiotApiClient;
import pl.kubit.tft_meta_hub.riot.RiotApiResult;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.KeyInvalid;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.NotFound;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Success;
import pl.kubit.tft_meta_hub.riot.dto.LeagueListDto;
import pl.kubit.tft_meta_hub.riot.dto.LeagueListDto.LeagueEntryDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchCollectorServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");
	private static final Instant GAME_END = Instant.parse("2026-11-24T20:00:00Z");

	@Mock
	private RiotApiClient riotApiClient;
	@Mock
	private GamePatchService gamePatchService;
	@Mock
	private TftMatchRepository matchRepository;
	@Mock
	private TrackedPlayerRepository trackedPlayerRepository;
	@Mock
	private CollectionRunRepository collectionRunRepository;

	private MatchCollectorService service;

	@BeforeEach
	void setUp() {
		service = serviceWithLimit(300);
		given(gamePatchService.getCalendar()).willReturn(RiotFixtures.PATCHES);
		given(collectionRunRepository.save(any())).will(returnsFirstArg());
		given(trackedPlayerRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(trackedPlayerRepository.findAllById(any())).willReturn(List.of());
		given(riotApiClient.getLeague(any(), any())).willReturn(new Success<>(new LeagueListDto("CHALLENGER", "RANKED_TFT", List.of())));
	}

	@Test
	void shouldSaveOnlyRankedMatchesOfCurrentSet_whenPlayerHasMixedGames() {
		onLadder("p1");
		matchIds("p1", "EUW1_1", "EUW1_2", "EUW1_3", "EUW1_4");
		match("EUW1_1", 1100, 18);
		match("EUW1_2", 1090, 18);
		match("EUW1_3", 1100, 17);
		given(riotApiClient.getMatch("EUW1_4")).willReturn(new NotFound<>());

		CollectionRun run = service.collect(RunTrigger.MANUAL);

		ArgumentCaptor<TftMatch> saved = ArgumentCaptor.forClass(TftMatch.class);
		then(matchRepository).should().save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo("EUW1_1");
		assertThat(saved.getValue().getPatch()).isEqualTo("18.2");
		assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(run.getMatchesSaved()).isEqualTo(1);
		assertThat(run.getMatchesSkipped()).isEqualTo(3);
		assertThat(run.getPlayersChecked()).isEqualTo(1);
	}

	@Test
	void shouldNotDownloadMatch_whenItIsAlreadyStored() {
		onLadder("p1");
		matchIds("p1", "EUW1_1");
		given(matchRepository.existsById("EUW1_1")).willReturn(true);

		CollectionRun run = service.collect(RunTrigger.MANUAL);

		then(riotApiClient).should(never()).getMatch(anyString());
		assertThat(run.getMatchesSkipped()).isEqualTo(1);
	}

	@Test
	void shouldDownloadSharedMatchOnce_whenTwoPlayersPlayedInTheSameLobby() {
		onLadder("p1", "p2");
		matchIds("p1", "EUW1_1");
		matchIds("p2", "EUW1_1");
		match("EUW1_1", 1100, 18);

		service.collect(RunTrigger.MANUAL);

		then(riotApiClient).should(times(1)).getMatch("EUW1_1");
	}

	@Test
	void shouldStopAtLimitAndNotMarkPlayerFetched_whenMoreMatchesAreAvailable() {
		service = serviceWithLimit(2);
		onLadder("p1");
		matchIds("p1", "EUW1_1", "EUW1_2", "EUW1_3");
		match("EUW1_1", 1100, 18);
		match("EUW1_2", 1100, 18);
		match("EUW1_3", 1100, 18);

		CollectionRun run = service.collect(RunTrigger.MANUAL);

		assertThat(run.getMatchesSaved()).isEqualTo(2);
		then(riotApiClient).should(never()).getMatch("EUW1_3");
		then(trackedPlayerRepository).should(never()).save(any());
	}

	@Test
	void shouldMarkPlayerFetchedAtRunStart_whenAllMatchIdsWereProcessed() {
		onLadder("p1");
		matchIds("p1");

		service.collect(RunTrigger.MANUAL);

		ArgumentCaptor<TrackedPlayer> player = ArgumentCaptor.forClass(TrackedPlayer.class);
		then(trackedPlayerRepository).should().save(player.capture());
		assertThat(player.getValue().getLastFetchedAt()).isEqualTo(NOW);
	}

	@Test
	void shouldAskOnlyForNewMatches_whenPlayerWasFetchedBefore() {
		TrackedPlayer known = TrackedPlayer.create("p1", Platform.EUW, NOW.minus(Duration.ofDays(1)));
		Instant lastFetch = Instant.parse("2026-11-24T06:00:00Z");
		known.markFetched(lastFetch);
		given(trackedPlayerRepository.findAllById(any())).willReturn(List.of(known));
		onLadder("p1", "p2");
		matchIds("p1");
		matchIds("p2");

		service.collect(RunTrigger.MANUAL);

		then(riotApiClient).should().getMatchIds("p1", lastFetch, 20);
		// a new player is read back as far as matches are kept
		then(riotApiClient).should().getMatchIds("p2", NOW.minus(Duration.ofDays(14)), 20);
	}

	@Test
	void shouldStopWholeRunWithKeyInvalid_whenRiotRejectsTheKey() {
		given(riotApiClient.getLeague(Platform.EUNE, LeagueTier.CHALLENGER)).willReturn(new KeyInvalid<>(403));

		CollectionRun run = service.collect(RunTrigger.MANUAL);

		assertThat(run.getStatus()).isEqualTo(RunStatus.KEY_INVALID);
		assertThat(run.getErrorMessage()).isEqualTo("Riot API key invalid or expired.");
		assertThat(run.getFinishedAt()).isEqualTo(NOW);
		then(riotApiClient).should(never()).getMatchIds(anyString(), any(), anyInt());
		then(matchRepository).should(never()).deleteAllPlayedBefore(any());
	}

	@Test
	void shouldStopWithKeyInvalid_whenKeyExpiresInTheMiddleOfTheRun() {
		onLadder("p1");
		matchIds("p1", "EUW1_1", "EUW1_2");
		match("EUW1_1", 1100, 18);
		given(riotApiClient.getMatch("EUW1_2")).willReturn(new KeyInvalid<>(401));

		CollectionRun run = service.collect(RunTrigger.MANUAL);

		assertThat(run.getStatus()).isEqualTo(RunStatus.KEY_INVALID);
		assertThat(run.getMatchesSaved()).isEqualTo(1);
	}

	@Test
	void shouldFailWithoutCallingRiot_whenNoPatchIsConfigured() {
		given(gamePatchService.getCalendar()).willReturn(new PatchCalendar(List.of()));

		CollectionRun run = service.collect(RunTrigger.SCHEDULED);

		assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
		assertThat(run.getErrorMessage()).isEqualTo(MatchCollectorService.NO_PATCH_MESSAGE);
		then(riotApiClient).shouldHaveNoInteractions();
	}

	@Test
	void shouldDeleteMatchesOlderThanRetention_whenRunSucceeds() {
		service.collect(RunTrigger.MANUAL);

		then(matchRepository).should().deleteAllPlayedBefore(NOW.minus(Duration.ofDays(14)));
	}

	private MatchCollectorService serviceWithLimit(int maxMatches) {
		return new MatchCollectorService(riotApiClient, gamePatchService, matchRepository, trackedPlayerRepository,
				collectionRunRepository, new CollectorProperties(maxMatches, 20, 14, false, "0 0 */6 * * *"),
				new MutableClock(NOW));
	}

	private void onLadder(String... puuids) {
		List<LeagueEntryDto> entries = Arrays.stream(puuids).map(puuid -> new LeagueEntryDto(puuid, 1000)).toList();
		given(riotApiClient.getLeague(Platform.EUW, LeagueTier.CHALLENGER))
				.willReturn(new Success<>(new LeagueListDto("CHALLENGER", "RANKED_TFT", entries)));
	}

	private void matchIds(String puuid, String... ids) {
		RiotApiResult<List<String>> result = new Success<>(List.of(ids));
		given(riotApiClient.getMatchIds(eq(puuid), any(), anyInt())).willReturn(result);
	}

	private void match(String matchId, int queueId, int setNumber) {
		RiotApiResult<MatchDto> result = new Success<>(RiotFixtures.match(matchId, queueId, setNumber, GAME_END));
		given(riotApiClient.getMatch(matchId)).willReturn(result);
	}
}
