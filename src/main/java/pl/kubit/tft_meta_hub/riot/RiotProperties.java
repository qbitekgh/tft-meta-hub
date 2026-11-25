package pl.kubit.tft_meta_hub.riot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Riot API settings.
 *
 * @param apiKey      from the {@code RIOT_API_KEY} environment variable; may be empty (the site works without
 *                    it, only collecting matches is impossible). Never logged.
 * @param hostUrl     URL template with a {@code {host}} placeholder, e.g. {@code https://{host}.api.riotgames.com}
 * @param regionalHost host of the match endpoints, e.g. "europe"
 * @param rateLimits  limits enforced per host, e.g. 20 requests / 1 s and 100 requests / 2 min
 * @param maxAttempts how many times a request is sent when Riot answers 429 Too Many Requests
 */
@Validated
@ConfigurationProperties(prefix = "app.riot")
public record RiotProperties(String apiKey,
							 @NotBlank String hostUrl,
							 @NotBlank String regionalHost,
							 @NotEmpty List<@Valid RateLimit> rateLimits,
							 @Min(1) int maxAttempts) {

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	/** At most {@code requests} requests in any time window of length {@code window}. */
	public record RateLimit(@Min(1) int requests, @NotNull Duration window) {
	}
}
