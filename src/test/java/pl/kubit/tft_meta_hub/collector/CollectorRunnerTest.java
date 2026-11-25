package pl.kubit.tft_meta_hub.collector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

class CollectorRunnerTest {

	private final MatchCollectorService collectorService = mock(MatchCollectorService.class);
	private final ExecutorService threads = Executors.newSingleThreadExecutor();

	@AfterEach
	void tearDown() {
		threads.shutdownNow();
	}

	@Test
	void shouldRefuseSecondRun_whenFirstIsStillInProgress() throws Exception {
		CountDownLatch runStarted = new CountDownLatch(1);
		CountDownLatch finishRun = new CountDownLatch(1);
		willAnswer(invocation -> {
			runStarted.countDown();
			finishRun.await(5, TimeUnit.SECONDS);
			return null;
		}).given(collectorService).collect(RunTrigger.MANUAL);
		CollectorRunner runner = new CollectorRunner(collectorService, new ConcurrentTaskExecutor(threads));

		assertThat(runner.startInBackground(RunTrigger.MANUAL)).isTrue();
		assertThat(runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(runner.isRunning()).isTrue();
		assertThat(runner.startInBackground(RunTrigger.SCHEDULED)).isFalse();

		finishRun.countDown();
		waitUntilIdle(runner);
		assertThat(runner.startInBackground(RunTrigger.MANUAL)).isTrue();
	}

	@Test
	void shouldAllowNextRun_whenPreviousRunCrashed() {
		given(collectorService.collect(RunTrigger.MANUAL)).willThrow(new IllegalStateException("boom"));
		// runs the task on the calling thread, so the test can check the flag right after
		CollectorRunner runner = new CollectorRunner(collectorService, Runnable::run);

		assertThat(runner.startInBackground(RunTrigger.MANUAL)).isTrue();

		assertThat(runner.isRunning()).isFalse();
		assertThat(runner.startInBackground(RunTrigger.MANUAL)).isTrue();
	}

	private static void waitUntilIdle(CollectorRunner runner) throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		while (runner.isRunning() && Instant.now().isBefore(deadline)) {
			Thread.sleep(10);
		}
		assertThat(runner.isRunning()).isFalse();
	}
}
