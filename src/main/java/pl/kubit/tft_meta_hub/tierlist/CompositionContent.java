package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.Item;

import java.util.List;

/**
 * Everything a composition consists of, with champions and items already loaded from the database.
 * Passed to {@link TierList} when a composition is added or changed; the entity validates it.
 */
public record CompositionContent(String name,
								 Tier tier,
								 int position,
								 PlayStyle playStyle,
								 Difficulty difficulty,
								 String tips,
								 List<UnitContent> units) {

	public record UnitContent(Champion champion, boolean carry, int starLevel, List<Item> items) {
	}
}
