package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ChampionInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ItemInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.TraitInfo;

/** Human-readable text of a filter, e.g. "Ahri 2–3★, 1–3 items" or "Brawler 4+". */
final class FilterLabels {

	private FilterLabels() {
	}

	static String label(ExplorerFilter filter, GameDataCatalog catalog) {
		return switch (filter) {
			case UnitFilter unit -> unitName(unit.unit(), catalog) + unitDetails(unit);
			case UnitItemFilter unitItem -> itemName(unitItem.item(), catalog) + " on " + unitName(unitItem.unit(), catalog);
			case ItemFilter item -> itemName(item.item(), catalog);
			case TraitFilter trait -> catalog.trait(trait.trait()).map(TraitInfo::name).orElse(trait.trait())
					+ " " + trait.minUnits() + "+";
		};
	}

	/** Only the parts that narrow the filter: "Ahri" for any stars and items. */
	private static String unitDetails(UnitFilter unit) {
		StringBuilder details = new StringBuilder();
		if (unit.minStars() != 1 || unit.maxStars() != ExplorerFilter.MAX_STARS) {
			details.append(' ').append(range(unit.minStars(), unit.maxStars())).append('★');
		}
		if (unit.minItems() != 0 || unit.maxItems() != ExplorerFilter.MAX_ITEMS) {
			details.append(details.isEmpty() ? " " : ", ")
					.append(range(unit.minItems(), unit.maxItems()))
					.append(unit.maxItems() == 1 ? " item" : " items");
		}
		return details.toString();
	}

	private static String range(int min, int max) {
		return min == max ? String.valueOf(min) : min + "–" + max;
	}

	private static String unitName(String apiName, GameDataCatalog catalog) {
		return catalog.champion(apiName).map(ChampionInfo::name).orElse(apiName);
	}

	private static String itemName(String apiName, GameDataCatalog catalog) {
		return catalog.item(apiName).map(ItemInfo::name).orElse(apiName);
	}
}
