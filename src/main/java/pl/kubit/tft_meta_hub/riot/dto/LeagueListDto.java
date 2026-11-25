package pl.kubit.tft_meta_hub.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Response of {@code /tft/league/v1/challenger} and {@code /grandmaster}; only the fields the collector needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeagueListDto(String tier, String queue, List<LeagueEntryDto> entries) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LeagueEntryDto(String puuid, int leaguePoints) {
	}
}
