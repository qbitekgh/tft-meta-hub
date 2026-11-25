package pl.kubit.tft_meta_hub.riot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Failure;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.KeyInvalid;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.NotFound;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.RateLimited;
import pl.kubit.tft_meta_hub.riot.RiotApiResult.Success;
import pl.kubit.tft_meta_hub.riot.dto.LeagueListDto;
import pl.kubit.tft_meta_hub.riot.dto.MatchDto;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Calls the three TFT endpoints the collector needs. Every request first waits for the rate limiter of its host;
 * the answer is turned into a {@link RiotApiResult} instead of an exception, so the caller decides what to do.
 * <p>
 * The API key is sent as the {@code X-Riot-Token} header (configured on the {@link RestClient}) and is never logged.
 * Logged URLs use templates without values, because some paths contain a PUUID.
 */
@Slf4j
@Component
public class RiotApiClient {

	static final String KEY_INVALID_MESSAGE = "Riot API key invalid or expired";
	private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(1);
	private static final ParameterizedTypeReference<List<String>> MATCH_IDS = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;
	private final RiotRateLimiter rateLimiter;
	private final RiotProperties properties;

	public RiotApiClient(@Qualifier("riotRestClient") RestClient restClient,
						 RiotRateLimiter rateLimiter,
						 RiotProperties properties) {
		this.restClient = restClient;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	/** {@code GET /tft/league/v1/{challenger|grandmaster}?queue=RANKED_TFT} on the platform host. */
	public RiotApiResult<LeagueListDto> getLeague(Platform platform, LeagueTier tier) {
		URI uri = UriComponentsBuilder.fromUriString(properties.hostUrl())
				.path("/tft/league/v1/{tier}")
				.queryParam("queue", "RANKED_TFT")
				.buildAndExpand(platform.getHost(), tier.getPath())
				.toUri();
		return get(platform.getHost(), uri, "league " + tier.getPath(), response -> response.bodyTo(LeagueListDto.class));
	}

	/**
	 * {@code GET /tft/match/v1/matches/by-puuid/{puuid}/ids} on the regional host.
	 *
	 * @param startTime only matches after this moment; {@code null} for no limit
	 */
	public RiotApiResult<List<String>> getMatchIds(String puuid, Instant startTime, int count) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.hostUrl())
				.path("/tft/match/v1/matches/by-puuid/{puuid}/ids")
				.queryParam("count", count);
		if (startTime != null) {
			builder.queryParam("startTime", startTime.getEpochSecond());
		}
		URI uri = builder.buildAndExpand(properties.regionalHost(), puuid).toUri();
		return get(properties.regionalHost(), uri, "match ids", response -> response.bodyTo(MATCH_IDS));
	}

	/** {@code GET /tft/match/v1/matches/{matchId}} on the regional host. */
	public RiotApiResult<MatchDto> getMatch(String matchId) {
		URI uri = UriComponentsBuilder.fromUriString(properties.hostUrl())
				.path("/tft/match/v1/matches/{matchId}")
				.buildAndExpand(properties.regionalHost(), matchId)
				.toUri();
		return get(properties.regionalHost(), uri, "match " + matchId, response -> response.bodyTo(MatchDto.class));
	}

	private <T> RiotApiResult<T> get(String host, URI uri, String description, BodyReader<T> bodyReader) {
		if (!properties.hasApiKey()) {
			log.error("{}: RIOT_API_KEY is not configured", KEY_INVALID_MESSAGE);
			return new KeyInvalid<>(0);
		}
		Duration retryAfter = DEFAULT_RETRY_AFTER;
		for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
			try {
				rateLimiter.acquire(host);
				RiotApiResult<T> result = restClient.get()
						.uri(uri)
						.exchange((request, response) -> toResult(response.getStatusCode(),
								response.getHeaders().getFirst("Retry-After"), () -> bodyReader.read(response)));
				if (!(result instanceof RateLimited<T>(Duration wait))) {
					return result;
				}
				retryAfter = wait;
				log.warn("Riot API returned 429 for {} on host {}; waiting {} s (attempt {}/{})",
						description, host, wait.toSeconds(), attempt, properties.maxAttempts());
				rateLimiter.blockFor(host, wait);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return new Failure<>("Interrupted while waiting for the rate limiter");
			} catch (RestClientException e) {
				log.warn("Riot API request for {} failed: {}", description, e.getMessage());
				return new Failure<>(e.getMessage());
			}
		}
		return new RateLimited<>(retryAfter);
	}

	private <T> RiotApiResult<T> toResult(HttpStatusCode status, String retryAfterHeader, BodySupplier<T> body)
			throws IOException {
		int code = status.value();
		if (status.is2xxSuccessful()) {
			return new Success<>(body.get());
		}
		return switch (code) {
			case 401, 403 -> {
				log.error("{} (HTTP {})", KEY_INVALID_MESSAGE, code);
				yield new KeyInvalid<>(code);
			}
			case 404 -> new NotFound<>();
			case 429 -> new RateLimited<>(parseRetryAfter(retryAfterHeader));
			default -> new Failure<>("HTTP " + code);
		};
	}

	/** Retry-After holds whole seconds; a missing or unreadable header means "wait a moment". */
	static Duration parseRetryAfter(String header) {
		if (header == null) {
			return DEFAULT_RETRY_AFTER;
		}
		try {
			return Duration.ofSeconds(Math.max(1, Long.parseLong(header.strip())));
		} catch (NumberFormatException e) {
			return DEFAULT_RETRY_AFTER;
		}
	}

	@FunctionalInterface
	private interface BodyReader<T> {
		T read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException;
	}

	@FunctionalInterface
	private interface BodySupplier<T> {
		T get() throws IOException;
	}
}
