package pl.kubit.tft_meta_hub.riot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.riot.RiotProperties.RateLimit;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RiotRateLimiterTest {

	private static final String HOST = "europe";
	private static final List<RateLimit> DEV_KEY_LIMITS = List.of(
			new RateLimit(20, Duration.ofSeconds(1)),
			new RateLimit(100, Duration.ofMinutes(2)));

	private MutableClock clock;
	private RiotRateLimiter limiter;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
		limiter = new RiotRateLimiter(DEV_KEY_LIMITS, clock, clock);
	}

	@Test
	void shouldAllowTwentyRequestsAndThenAskToWait_whenBurstHitsShortWindow() {
		IntStream.range(0, 20).forEach(i -> assertThat(limiter.tryAcquire(HOST)).isZero());

		assertThat(limiter.tryAcquire(HOST)).isEqualTo(Duration.ofSeconds(1));
	}

	@Test
	void shouldAllowAgain_whenShortWindowHasPassed() {
		IntStream.range(0, 20).forEach(i -> limiter.tryAcquire(HOST));

		clock.advance(Duration.ofSeconds(1));

		assertThat(limiter.tryAcquire(HOST)).isZero();
	}

	@Test
	void shouldWaitForLongWindow_whenHundredRequestsWereMadeInTwoMinutes() {
		for (int second = 0; second < 5; second++) {
			IntStream.range(0, 20).forEach(i -> assertThat(limiter.tryAcquire(HOST)).isZero());
			clock.advance(Duration.ofSeconds(1));
		}

		// 100 requests in the first 5 seconds; the oldest leaves the 2-minute window at 12:02:00
		assertThat(limiter.tryAcquire(HOST)).isEqualTo(Duration.ofMinutes(2).minusSeconds(5));
	}

	@Test
	void shouldNotCountRequest_whenItHadToWait() {
		IntStream.range(0, 20).forEach(i -> limiter.tryAcquire(HOST));
		IntStream.range(0, 5).forEach(i -> limiter.tryAcquire(HOST));

		clock.advance(Duration.ofSeconds(1));

		IntStream.range(0, 20).forEach(i -> assertThat(limiter.tryAcquire(HOST)).isZero());
	}

	@Test
	void shouldCountEachHostSeparately_whenOneHostIsFull() {
		IntStream.range(0, 20).forEach(i -> limiter.tryAcquire("eun1"));

		assertThat(limiter.tryAcquire("eun1")).isPositive();
		assertThat(limiter.tryAcquire("euw1")).isZero();
		assertThat(limiter.tryAcquire("europe")).isZero();
	}

	@Test
	void shouldBlockHostUntilRetryAfterPasses_whenRiotAnswered429() {
		limiter.blockFor(HOST, Duration.ofSeconds(7));

		assertThat(limiter.tryAcquire(HOST)).isEqualTo(Duration.ofSeconds(7));
		assertThat(limiter.tryAcquire("eun1")).isZero();

		clock.advance(Duration.ofSeconds(7));
		assertThat(limiter.tryAcquire(HOST)).isZero();
	}

	@Test
	void shouldSleepOnlyAsLongAsNeeded_whenAcquireMustWait() throws InterruptedException {
		for (int i = 0; i < 21; i++) {
			limiter.acquire(HOST);
		}

		assertThat(clock.totalSlept()).isEqualTo(Duration.ofSeconds(1));
	}

	@Test
	void shouldGrantExactlyTheLimit_whenManyThreadsAskAtTheSameMoment() throws Exception {
		int threads = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Duration>> results = IntStream.range(0, threads)
					.mapToObj(i -> executor.submit(() -> {
						start.await();
						return limiter.tryAcquire(HOST);
					}))
					.toList();
			start.countDown();

			long granted = 0;
			for (Future<Duration> result : results) {
				if (result.get(5, TimeUnit.SECONDS).isZero()) {
					granted++;
				}
			}
			assertThat(granted).isEqualTo(20);
		} finally {
			executor.shutdownNow();
		}
	}
}
