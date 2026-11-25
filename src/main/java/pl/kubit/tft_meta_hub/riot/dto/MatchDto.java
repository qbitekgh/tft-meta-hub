package pl.kubit.tft_meta_hub.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of {@code /tft/match/v1/matches/{matchId}}, reduced to the fields the statistics need.
 * <p>
 * Player identity (PUUID, Riot ID, game name) is deliberately not mapped at all: the application stores
 * and shows only aggregated statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchDto(MetadataDto metadata, InfoDto info) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MetadataDto(@JsonProperty("match_id") String matchId) {
	}

	/**
	 * @param gameDatetime epoch milliseconds when the game ended
	 * @param queueId      1100 = ranked TFT
	 * @param gameVersion  a version text; since set 18 it is "TFT Unreal Version ?.?.?.?" without a patch number
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InfoDto(@JsonProperty("game_datetime") long gameDatetime,
						  @JsonProperty("game_version") String gameVersion,
						  @JsonProperty("queue_id") int queueId,
						  @JsonProperty("tft_set_number") int tftSetNumber,
						  List<ParticipantDto> participants) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ParticipantDto(int placement, int level, List<UnitDto> units, List<TraitDto> traits) {
	}

	/**
	 * @param characterId same value as the champion's api name in Community Dragon, e.g. "DA_18_Rakan"
	 * @param starLevel   called "tier" in the API: 1–3 stars
	 * @param itemNames   item api names, e.g. "DA_GuinsoosRageblade"
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UnitDto(@JsonProperty("character_id") String characterId,
						  @JsonProperty("tier") int starLevel,
						  List<String> itemNames) {
	}

	/**
	 * @param tierCurrent active level of the trait; 0 means the trait is listed but not active
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TraitDto(String name,
						   @JsonProperty("num_units") int numUnits,
						   @JsonProperty("tier_current") int tierCurrent,
						   int style) {

		public boolean isActive() {
			return tierCurrent > 0;
		}
	}
}
