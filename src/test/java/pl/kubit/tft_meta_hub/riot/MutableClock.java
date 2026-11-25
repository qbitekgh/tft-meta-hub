package pl.kubit.tft_meta_hub.riot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that stands still until a test moves it forward; also usable as a {@link Sleeper} that "waits" instantly. */
public final class MutableClock extends Clock implements Sleeper {

	private volatile Instant now;
	private volatile Duration totalSlept = Duration.ZERO;

	public MutableClock(Instant start) {
		this.now = start;
	}

	public synchronized void advance(Duration duration) {
		now = now.plus(duration);
	}

	@Override
	public synchronized void sleep(Duration duration) {
		totalSlept = totalSlept.plus(duration);
		advance(duration);
	}

	public Duration totalSlept() {
		return totalSlept;
	}

	@Override
	public Instant instant() {
		return now;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}
