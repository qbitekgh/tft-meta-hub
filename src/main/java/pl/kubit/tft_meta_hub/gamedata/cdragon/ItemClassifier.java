package pl.kubit.tft_meta_hub.gamedata.cdragon;

import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonItem;

import java.util.List;
import java.util.Optional;

/**
 * Decides which items of a set are worth importing. The data file lists several hundred "items" per set,
 * most of them technical (shop offers, consumables, set-mechanic tokens); only items a unit can hold
 * at the end of a game are kept. The rules are checked in order, the first match wins.
 */
final class ItemClassifier {

	private static final List<String> SKIPPED_API_NAME_PARTS = List.of("Consumable", "Assist", "MarketOffering");

	private ItemClassifier() {
	}

	static Optional<ItemCategory> classify(CdragonItem item) {
		if (isSkipped(item)) {
			return Optional.empty();
		}
		if (hasTag(item, "component")) {
			return Optional.of(ItemCategory.COMPONENT);
		}
		if (item.name().endsWith(" Emblem")) {
			return Optional.of(ItemCategory.EMBLEM);
		}
		if (item.composition() != null && item.composition().size() == 2) {
			return Optional.of(ItemCategory.COMPLETED);
		}
		if (item.apiName().contains("Item_Artifact_")) {
			return Optional.of(ItemCategory.ARTIFACT);
		}
		if (item.name().startsWith("Radiant ")) {
			return Optional.of(ItemCategory.RADIANT);
		}
		return Optional.empty();
	}

	private static boolean isSkipped(CdragonItem item) {
		return item.name() == null
				|| item.augment()
				|| hasTag(item, "Consumable")
				|| SKIPPED_API_NAME_PARTS.stream().anyMatch(part -> item.apiName().contains(part));
	}

	private static boolean hasTag(CdragonItem item, String tag) {
		return item.tags() != null && item.tags().contains(tag);
	}
}
