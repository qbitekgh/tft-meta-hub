package pl.kubit.tft_meta_hub.collector;

import java.time.Instant;

/**
 * @param running       whether a run is in progress right now
 * @param lastRun       the most recent run, null if the collector never ran
 * @param storedMatches matches currently in the database
 * @param latestGameAt  end time of the newest stored match, null when there are none
 */
public record CollectorStatus(boolean running, LastRun lastRun, long storedMatches, Instant latestGameAt) {

	public record LastRun(RunTrigger trigger,
						  RunStatus status,
						  Instant startedAt,
						  Instant finishedAt,
						  int playersChecked,
						  int matchesSaved,
						  int matchesSkipped,
						  String errorMessage) {
	}
}
