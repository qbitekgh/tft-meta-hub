package pl.kubit.tft_meta_hub.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts collection runs in the background, one at a time.
 * <p>
 * {@link AtomicBoolean#compareAndSet} makes "is a run in progress? if not, mark it as started" a single atomic step,
 * so two admins clicking "Collect now" at the same moment (or the scheduler and a click) cannot both start a run.
 * The run itself executes on a separate thread, so the web request returns immediately.
 */
@Slf4j
@Component
public class CollectorRunner {

	private final MatchCollectorService collectorService;
	private final TaskExecutor executor;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public CollectorRunner(MatchCollectorService collectorService,
						   @Qualifier("collectorExecutor") TaskExecutor executor) {
		this.collectorService = collectorService;
		this.executor = executor;
	}

	/** @return false when a run is already in progress (nothing is started then) */
	public boolean startInBackground(RunTrigger trigger) {
		if (!running.compareAndSet(false, true)) {
			return false;
		}
		try {
			executor.execute(() -> {
				try {
					collectorService.collect(trigger);
				} catch (RuntimeException e) {
					log.error("Collection run crashed", e);
				} finally {
					running.set(false);
				}
			});
			return true;
		} catch (RuntimeException e) {
			running.set(false);
			throw e;
		}
	}

	public boolean isRunning() {
		return running.get();
	}
}
