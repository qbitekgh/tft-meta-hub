package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;
import pl.kubit.tft_meta_hub.gamedata.Item;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ItemOption;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ItemChoicesTest {

	@Test
	void shouldGroupByCategoryWithoutComponents_whenBuildingFormChoices() {
		List<Item> items = List.of(
				item(18, "TFT_Item_InfinityEdge", "Infinity Edge", ItemCategory.COMPLETED),
				item(18, "DA_Component_BFSword", "B.F. Sword", ItemCategory.COMPONENT),
				item(18, "TFT_Item_Artifact_Fishbones", "Fishbones", ItemCategory.ARTIFACT),
				item(18, "DA_InfinityEdge", "Infinity Edge", ItemCategory.COMPLETED),
				item(18, "DA_Deathblade", "Deathblade", ItemCategory.COMPLETED),
				item(18, "DA_18_EmblemFae", "Fae Emblem", ItemCategory.EMBLEM));

		Map<ItemCategory, List<ItemOption>> choices = ItemChoices.forCompositions(items, 18);

		assertThat(choices).containsOnlyKeys(ItemCategory.COMPLETED, ItemCategory.EMBLEM, ItemCategory.ARTIFACT);
		assertThat(choices.keySet()).containsExactly(ItemCategory.COMPLETED, ItemCategory.EMBLEM, ItemCategory.ARTIFACT);
		assertThat(choices.get(ItemCategory.COMPLETED))
				.extracting(ItemOption::name)
				.containsExactly("Deathblade", "Infinity Edge");
	}

	private long nextId = 1;

	/** Items get ids the way the database would assign them; there is no setter on purpose. */
	private Item item(int setNumber, String apiName, String name, ItemCategory category) {
		Item item = Item.create(setNumber, new ItemData(apiName, name, null, category));
		ReflectionTestUtils.setField(item, "id", nextId++);
		return item;
	}
}
