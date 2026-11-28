package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.gamedata.SetItems;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ItemOption;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the item choices for the composition form.
 * <ul>
 *     <li>Components are left out: a final board has completed items.</li>
 *     <li>Generic twins of set-specific items are left out ({@link SetItems}), so every name appears once.</li>
 * </ul>
 */
final class ItemChoices {

	private ItemChoices() {
	}

	static Map<ItemCategory, List<ItemOption>> forCompositions(List<Item> items, int setNumber) {
		Map<ItemCategory, List<ItemOption>> byCategory = new EnumMap<>(ItemCategory.class);
		SetItems.withoutGenericDuplicates(items, setNumber).stream()
				.filter(item -> item.getCategory() != ItemCategory.COMPONENT)
				.sorted(Comparator.comparing(Item::getName))
				.forEach(item -> byCategory
						.computeIfAbsent(item.getCategory(), category -> new ArrayList<>())
						.add(new ItemOption(item.getId(), item.getName())));
		return byCategory;
	}
}
