package pl.kubit.tft_meta_hub.collector;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CollectorProperties.class)
class CollectorConfig {

	/**
	 * A single background thread for collection runs. With no queue, a second task is rejected instead of waiting;
	 * {@link CollectorRunner} never submits one while a run is in progress anyway.
	 */
	@Bean
	ThreadPoolTaskExecutor collectorExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("collector-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		return executor;
	}
}
