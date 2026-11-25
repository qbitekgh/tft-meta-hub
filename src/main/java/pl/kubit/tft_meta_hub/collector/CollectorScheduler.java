package pl.kubit.tft_meta_hub.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional automatic runs. The bean exists only when {@code app.collector.schedule-enabled=true}
 * (off by default: a development key expires every 24 hours, so unattended runs would mostly fail).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.collector", name = "schedule-enabled", havingValue = "true")
class CollectorScheduler {

	private final CollectorRunner collectorRunner;

	@Scheduled(cron = "${app.collector.schedule-cron}")
	void runScheduledCollection() {
		if (!collectorRunner.startInBackground(RunTrigger.SCHEDULED)) {
			log.info("Scheduled collection skipped: a run is already in progress");
		}
	}
}
