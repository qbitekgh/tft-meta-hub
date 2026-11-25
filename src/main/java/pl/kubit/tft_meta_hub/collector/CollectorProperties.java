package pl.kubit.tft_meta_hub.collector;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxMatchesPerRun  a run stops after saving this many new matches
 * @param matchIdsPerPlayer how many of a player's newest match ids are requested
 * @param retentionDays     matches older than this are deleted after every run
 * @param scheduleEnabled   whether the {@code @Scheduled} job runs at all (off by default)
 * @param scheduleCron      when the scheduled job runs, e.g. every 6 hours
 */
@Validated
@ConfigurationProperties(prefix = "app.collector")
public record CollectorProperties(@Min(1) int maxMatchesPerRun,
								  @Min(1) @Max(200) int matchIdsPerPlayer,
								  @Min(1) int retentionDays,
								  boolean scheduleEnabled,
								  @NotBlank String scheduleCron) {
}
