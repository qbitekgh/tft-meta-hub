package pl.kubit.tft_meta_hub.riot;

import java.time.Duration;

/**
 * Waiting, as a replaceable dependency. Production uses {@code Thread.sleep}; tests pass an implementation
 * that only moves a test clock forward, so they run instantly and deterministically.
 */
@FunctionalInterface
public interface Sleeper {

	void sleep(Duration duration) throws InterruptedException;

	Sleeper THREAD_SLEEP = duration -> Thread.sleep(duration.toMillis());
}
