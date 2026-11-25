package pl.kubit.tft_meta_hub.tierlist;

import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ItemOption;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the item choices for the composition form.
 * <ul>
 *     <li>Components are left out: a final board has completed items.</li>
 *     <li>Some sets contain two items with the same name, a set-specific one (e.g. {@code DA_GuinsoosRageblade}
 *     in set 18) and a generic twin ({@code TFT_Item_GuinsoosRageblade}). Only the set-specific one is offered,
 *     so every name appears once.</li>
 * </ul>
 */
final class ItemChoices {

	private ItemChoices() {
	}

	static Map<ItemCategory, List<ItemOption>> forCompositions(List<Item> items, int setNumber) {
		Map<ItemCategory, List<ItemOption>> byCategory = new EnumMap<>(ItemCategory.class);
		withoutGenericDuplicates(items, setNumber).stream()
				.filter(item -> item.getCategory() != ItemCategory.COMPONENT)
				.sorted(Comparator.comparing(Item::getName))
				.forEach(item -> byCategory
						.computeIfAbsent(item.getCategory(), category -> new ArrayList<>())
						.add(new ItemOption(item.getId(), item.getName())));
		return byCategory;
	}

	static List<Item> withoutGenericDuplicates(List<Item> items, int setNumber) {
		Map<String, List<Item>> byName = items.stream()
				.collect(Collectors.groupingBy(Item::getName, LinkedHashMap::new, Collectors.toList()));
		return byName.values().stream()
				.flatMap(sameName -> preferSetSpecific(sameName, setNumber).stream())
				.toList();
	}

	private static List<Item> preferSetSpecific(List<Item> sameName, int setNumber) {
		if (sameName.size() == 1) {
			return sameName;
		}
		List<Item> setSpecific = sameName.stream()
				.filter(item -> !isGeneric(item, setNumber))
				.toList();
		return setSpecific.isEmpty() ? sameName : setSpecific;
	}

	/**
	 * Generic items have a {@code TFT} prefix that is not the current set's own one,
	 * e.g. {@code TFT_Item_...} or {@code TFT5_Item_...Radiant}, but not {@code TFT17_...} in set 17.
	 */
	private static boolean isGeneric(Item item, int setNumber) {
		String apiName = item.getApiName();
		return apiName.startsWith("TFT") && !apiName.startsWith("TFT" + setNumber + "_");
	}
}
