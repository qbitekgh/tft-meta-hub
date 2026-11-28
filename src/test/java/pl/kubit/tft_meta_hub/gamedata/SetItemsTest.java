package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SetItemsTest {

	@Test
	void shouldKeepSetSpecificItem_whenGenericTwinHasSameName() {
		List<Item> items = List.of(
				item(18, "TFT_Item_GuinsoosRageblade", "Guinsoo's Rageblade", ItemCategory.COMPLETED),
				item(18, "DA_GuinsoosRageblade", "Guinsoo's Rageblade", ItemCategory.COMPLETED),
				item(18, "TFT5_Item_GuinsoosRagebladeRadiant", "Radiant Guinsoo's Rageblade", ItemCategory.RADIANT),
				item(18, "DA_GuinsoosRagebladeRadiant", "Radiant Guinsoo's Rageblade", ItemCategory.RADIANT));

		assertThat(SetItems.withoutGenericDuplicates(items, 18))
				.extracting(Item::getApiName)
				.containsExactly("DA_GuinsoosRageblade", "DA_GuinsoosRagebladeRadiant");
	}

	@Test
	void shouldKeepGenericItem_whenItsNameIsUnique() {
		List<Item> items = List.of(item(18, "TFT_Item_Artifact_Fishbones", "Fishbones", ItemCategory.ARTIFACT));

		assertThat(SetItems.withoutGenericDuplicates(items, 18))
				.extracting(Item::getApiName)
				.containsExactly("TFT_Item_Artifact_Fishbones");
	}

	@Test
	void shouldTreatOwnSetPrefixAsSetSpecific_whenSetUsesTftPrefix() {
		List<Item> items = List.of(
				item(17, "TFT_Item_Something", "Same Name", ItemCategory.COMPLETED),
				item(17, "TFT17_Item_Something", "Same Name", ItemCategory.COMPLETED));

		assertThat(SetItems.withoutGenericDuplicates(items, 17))
				.extracting(Item::getApiName)
				.containsExactly("TFT17_Item_Something");
	}

	private static Item item(int setNumber, String apiName, String name, ItemCategory category) {
		return Item.create(setNumber, new ItemData(apiName, name, null, category));
	}
}
