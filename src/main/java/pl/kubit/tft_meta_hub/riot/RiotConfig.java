package pl.kubit.tft_meta_hub.riot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RiotProperties.class)
class RiotConfig {

	/** Every request carries the API key in the X-Riot-Token header; URLs are absolute (several hosts). */
	@Bean
	RestClient riotRestClient(RiotProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(20));

		RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
		if (properties.hasApiKey()) {
			builder.defaultHeader("X-Riot-Token", properties.apiKey());
		}
		return builder.build();
	}

	/** One limiter for the whole application, so all threads share the same counters. */
	@Bean
	RiotRateLimiter riotRateLimiter(RiotProperties properties, Clock clock) {
		return new RiotRateLimiter(properties.rateLimits(), clock, Sleeper.THREAD_SLEEP);
	}
}
