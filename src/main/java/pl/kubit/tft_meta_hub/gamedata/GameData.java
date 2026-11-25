package pl.kubit.tft_meta_hub.gamedata;

import java.util.List;

/**
 * Game data of one set, already filtered and converted from the external source format.
 */
public record GameData(int setNumber, List<TraitData> traits, List<ChampionData> champions, List<ItemData> items) {

	public record TraitData(String apiName, String name, String iconUrl, List<Integer> breakpoints) {
	}

	public record ChampionData(String apiName, String name, int cost, String iconUrl, List<String> traitNames) {
	}

	public record ItemData(String apiName, String name, String iconUrl, ItemCategory category) {
	}
}
