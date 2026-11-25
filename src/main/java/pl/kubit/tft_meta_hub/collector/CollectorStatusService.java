package pl.kubit.tft_meta_hub.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** What the admin panel shows about the collector, and clean-up of runs cut off by a restart. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorStatusService {

	private final CollectorRunner collectorRunner;
	private final CollectionRunRepository collectionRunRepository;
	private final TftMatchRepository matchRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public CollectorStatus getStatus() {
		CollectorStatus.LastRun lastRun = collectionRunRepository.findFirstByOrderByStartedAtDesc()
				.map(run -> new CollectorStatus.LastRun(run.getTrigger(), run.getStatus(), run.getStartedAt(),
						run.getFinishedAt(), run.getPlayersChecked(), run.getMatchesSaved(), run.getMatchesSkipped(),
						run.getErrorMessage()))
				.orElse(null);
		return new CollectorStatus(collectorRunner.isRunning(), lastRun, matchRepository.count(),
				matchRepository.findLatestGameDatetime());
	}

	/** A run still marked RUNNING when the application starts was interrupted by the previous shutdown. */
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void markInterruptedRuns() {
		collectionRunRepository.findAllByStatus(RunStatus.RUNNING).forEach(run -> {
			run.markInterrupted(clock.instant());
			log.warn("Collection run {} was interrupted by a restart", run.getId());
		});
	}
}
