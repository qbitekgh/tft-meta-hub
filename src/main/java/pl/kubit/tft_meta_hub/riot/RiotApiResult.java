package pl.kubit.tft_meta_hub.riot;

import java.time.Duration;

/**
 * Outcome of one Riot API call. The interface is sealed, so a {@code switch} over a result must handle
 * every case and the compiler reports a missing one:
 * <pre>{@code
 * switch (client.getMatch(id)) {
 *     case Success<MatchDto>(MatchDto match) -> save(match);
 *     case NotFound<MatchDto> notFound -> skip();
 *     case KeyInvalid<MatchDto> keyInvalid -> stopRun();
 *     case RateLimited<MatchDto> rateLimited -> stopRun();
 *     case Failure<MatchDto>(String message) -> logAndContinue(message);
 * }
 * }</pre>
 */
public sealed interface RiotApiResult<T> {

	record Success<T>(T body) implements RiotApiResult<T> {
	}

	/** HTTP 404, e.g. a match id that does not exist. */
	record NotFound<T>() implements RiotApiResult<T> {
	}

	/** HTTP 401/403 or no key configured: a development key expires every 24 hours. Retrying does not help. */
	record KeyInvalid<T>(int status) implements RiotApiResult<T> {
	}

	/** HTTP 429 even after waiting for {@code Retry-After} the configured number of times. */
	record RateLimited<T>(Duration retryAfter) implements RiotApiResult<T> {
	}

	/** Any other error: HTTP 5xx, network problem, unreadable response. */
	record Failure<T>(String message) implements RiotApiResult<T> {
	}
}
