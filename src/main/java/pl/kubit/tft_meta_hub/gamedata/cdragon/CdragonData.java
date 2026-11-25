package pl.kubit.tft_meta_hub.gamedata.cdragon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The part of Community Dragon's {@code cdragon/tft/en_us.json} that the import needs.
 * Every other field of the (24 MB) file is ignored.
 *
 * @param items   items of all sets and game modes
 * @param setData one entry per game mode; the ranked mode of set N has mutator {@code TFTSet<N>}
 * @param sets    set number (as text, e.g. "18") to its champions and traits
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CdragonData(List<CdragonItem> items, List<CdragonSetData> setData, Map<String, CdragonSet> sets) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonSet(String name, List<CdragonChampion> champions, List<CdragonTrait> traits) {
	}

	/** {@code traits} contains display names of traits (e.g. "Rogue"), not their api names. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonChampion(String apiName, String name, int cost, String tileIcon, List<String> traits) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonTrait(String apiName, String name, String icon, List<CdragonTraitEffect> effects) {
	}

	/** {@code minUnits} is null for a few special traits (e.g. set 18 "Eclipse"), so it is not a primitive. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonTraitEffect(Integer minUnits) {
	}

	/** {@code items} contains api names of the items available in this game mode. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonSetData(int number, String mutator, List<String> items) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CdragonItem(String apiName,
							  String name,
							  String icon,
							  List<String> composition,
							  List<String> tags,
							  @JsonProperty("isAugment") boolean augment) {
	}
}
