package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.Champion;
import pl.kubit.tft_meta_hub.gamedata.GameData.ChampionData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.tierlist.CompositionContent.UnitContent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/** Builders of game data and composition content for tier list tests. */
final class TierListFixtures {

	static final int SET = 18;
	static final Instant CREATED_AT = Instant.parse("2026-11-20T10:00:00Z");
	static final Instant LATER = Instant.parse("2026-11-25T12:00:00Z");

	private TierListFixtures() {
	}

	static Champion champion(String name, int cost) {
		return champion(name, cost, SET);
	}

	static Champion champion(String name, int cost, int setNumber) {
		return Champion.create(setNumber, new ChampionData("DA_18_" + name, name, cost, null, List.of()), Set.of());
	}

	static Item item(String name) {
		return item(name, SET);
	}

	static Item item(String name, int setNumber) {
		return Item.create(setNumber, new ItemData("DA_" + name.replace(" ", ""), name, null, ItemCategory.COMPLETED));
	}

	static UnitContent unit(Champion champion, Item... items) {
		return new UnitContent(champion, false, 2, List.of(items));
	}

	static CompositionContent composition(String name, Tier tier, int position, List<UnitContent> units) {
		return new CompositionContent(name, tier, position, PlayStyle.FAST_8, Difficulty.MEDIUM, "Tips", units);
	}

	static CompositionContent composition(String name, Tier tier, int position) {
		return composition(name, tier, position, List.of(unit(champion("Xayah", 1))));
	}

	static List<UnitContent> units(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> unit(champion("Champion" + i, 1)))
				.toList();
	}

	static TierList draft() {
		return TierList.createDraft("Patch 18.1 tier list", "18.1", SET, CREATED_AT);
	}
}
