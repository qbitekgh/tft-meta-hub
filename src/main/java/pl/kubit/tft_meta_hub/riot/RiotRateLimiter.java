package pl.kubit.tft_meta_hub.riot;

import lombok.extern.slf4j.Slf4j;
import pl.kubit.tft_meta_hub.riot.RiotProperties.RateLimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps requests within Riot's limits (e.g. 20 per 1 s and 100 per 2 min), separately for every host,
 * because Riot counts each routing host ({@code eun1}, {@code euw1}, {@code europe}) on its own.
 * <p>
 * Algorithm: a sliding window log. Each host remembers the times of its recent requests. A new request
 * is allowed when, for every limit, fewer than {@code requests} requests happened within the last
 * {@code window}; otherwise the caller waits until the oldest of those requests leaves the window.
 * <p>
 * Thread safety: hosts live in a {@link ConcurrentHashMap} and each host's state is changed only inside
 * {@code synchronized} methods of {@link HostWindow}, so two threads can never both take the last free slot.
 */
@Slf4j
public class RiotRateLimiter {

	private final List<RateLimit> limits;
	private final Duration longestWindow;
	private final Clock clock;
	private final Sleeper sleeper;
	private final Map<String, HostWindow> hosts = new ConcurrentHashMap<>();

	public RiotRateLimiter(List<RateLimit> limits, Clock clock, Sleeper sleeper) {
		this.limits = List.copyOf(limits);
		this.longestWindow = limits.stream().map(RateLimit::window).max(Comparator.naturalOrder()).orElseThrow();
		this.clock = clock;
		this.sleeper = sleeper;
	}

	/** Blocks until a request to {@code host} is allowed, then counts it. */
	public void acquire(String host) throws InterruptedException {
		Duration wait;
		while (!(wait = tryAcquire(host)).isZero()) {
			log.debug("Rate limit reached for host {}, waiting {} ms", host, wait.toMillis());
			sleeper.sleep(wait);
		}
	}

	/**
	 * Counts the request and returns {@link Duration#ZERO} when it is allowed now; otherwise counts nothing
	 * and returns how long to wait before trying again.
	 */
	public Duration tryAcquire(String host) {
		return window(host).tryAcquire(clock.instant());
	}

	/** After HTTP 429: no request to {@code host} is allowed for {@code duration} (the Retry-After value). */
	public void blockFor(String host, Duration duration) {
		window(host).blockUntil(clock.instant().plus(duration));
	}

	private HostWindow window(String host) {
		return hosts.computeIfAbsent(host, ignored -> new HostWindow());
	}

	private class HostWindow {

		/** Times of recent requests, oldest first. */
		private final Deque<Instant> requestTimes = new ArrayDeque<>();
		private Instant blockedUntil = Instant.MIN;

		synchronized Duration tryAcquire(Instant now) {
			if (now.isBefore(blockedUntil)) {
				return Duration.between(now, blockedUntil);
			}
			forgetRequestsOlderThan(now.minus(longestWindow));

			Duration wait = Duration.ZERO;
			for (RateLimit limit : limits) {
				Duration waitForLimit = waitFor(limit, now);
				if (waitForLimit.compareTo(wait) > 0) {
					wait = waitForLimit;
				}
			}
			if (wait.isZero()) {
				requestTimes.addLast(now);
			}
			return wait;
		}

		synchronized void blockUntil(Instant until) {
			if (until.isAfter(blockedUntil)) {
				blockedUntil = until;
			}
		}

		/**
		 * If the window is full, the request that must leave it first is the one {@code requests} positions
		 * from the newest; the caller can go when that request is older than the window.
		 */
		private Duration waitFor(RateLimit limit, Instant now) {
			Instant windowStart = now.minus(limit.window());
			List<Instant> inWindow = requestTimes.stream()
					.filter(time -> time.isAfter(windowStart))
					.toList();
			if (inWindow.size() < limit.requests()) {
				return Duration.ZERO;
			}
			Instant blockingRequest = inWindow.get(inWindow.size() - limit.requests());
			return Duration.between(now, blockingRequest.plus(limit.window()));
		}

		private void forgetRequestsOlderThan(Instant limitTime) {
			while (!requestTimes.isEmpty() && !requestTimes.peekFirst().isAfter(limitTime)) {
				requestTimes.removeFirst();
			}
		}
	}
}
