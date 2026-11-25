package pl.kubit.tft_meta_hub.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Code asks the injected {@link Clock} for the current time instead of calling {@code Instant.now()},
 * so tests can use a fixed clock.
 */
@Configuration
class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
