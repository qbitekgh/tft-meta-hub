package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.ItemCategory;

import java.util.List;
import java.util.Map;

/**
 * Choices for the composition form of one tier list: champions and items of its set.
 *
 * @param itemsByCategory items grouped by category in the order of {@link ItemCategory} (an EnumMap)
 */
public record CompositionFormOptions(long tierListId,
									 String tierListTitle,
									 int setNumber,
									 List<ChampionOption> champions,
									 Map<ItemCategory, List<ItemOption>> itemsByCategory) {

	public record ChampionOption(long id, String name, int cost) {
	}

	public record ItemOption(long id, String name) {
	}
}
