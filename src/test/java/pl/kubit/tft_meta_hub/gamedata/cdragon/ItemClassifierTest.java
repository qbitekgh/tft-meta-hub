package pl.kubit.tft_meta_hub.gamedata.cdragon;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.gamedata.cdragon.CdragonData.CdragonItem;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemClassifierTest {

	private static final List<String> TWO_COMPONENTS = List.of("DA_Component_RecurveBow", "DA_Component_NeedlesslyLargeRod");

	@Test
	void shouldReturnComponent_whenItemHasComponentTag() {
		CdragonItem item = item("DA_Component_BFSword", "B.F. Sword", List.of(), List.of("component"));

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.COMPONENT);
	}

	@Test
	void shouldReturnCompleted_whenItemIsBuiltFromTwoComponents() {
		CdragonItem item = item("DA_GuinsoosRageblade", "Guinsoo's Rageblade", TWO_COMPONENTS, List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.COMPLETED);
	}

	@Test
	void shouldReturnEmblem_whenCraftableItemNameEndsWithEmblem() {
		CdragonItem item = item("DA_18_EmblemFae", "Fae Emblem", List.of("DA_Component_Spatula", "DA_Component_RecurveBow"), List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.EMBLEM);
	}

	@Test
	void shouldReturnArtifact_whenApiNameContainsItemArtifact() {
		CdragonItem item = item("TFT_Item_Artifact_Fishbones", "Fishbones", List.of(), List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.ARTIFACT);
	}

	@Test
	void shouldReturnArtifact_whenArtifactHasSetSpecificPrefix() {
		CdragonItem item = item("DA_Artifact_GoldCollector", "Gold Collector", List.of(), List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.ARTIFACT);
	}

	@Test
	void shouldReturnArtifact_whenArtifactNameStartsWithRadiant() {
		CdragonItem item = item("TFT17_Item_Artifact_KayleArtifact_Radiant", "Radiant Exaltation", List.of(), List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.ARTIFACT);
	}

	@Test
	void shouldReturnRadiant_whenNameStartsWithRadiant() {
		CdragonItem item = item("DA_GuinsoosRagebladeRadiant", "Radiant Guinsoo's Rageblade", List.of(), List.of());

		assertThat(ItemClassifier.classify(item)).contains(ItemCategory.RADIANT);
	}

	@Test
	void shouldSkip_whenItemIsConsumableAssistOrShopOffer() {
		assertThat(ItemClassifier.classify(item("DA_Consumable_ItemRemover", "Item Remover", List.of(), List.of("Consumable")))).isEmpty();
		assertThat(ItemClassifier.classify(item("TFT_Assist_RandomRadiantItem", "Radiant Item", List.of(), List.of()))).isEmpty();
		assertThat(ItemClassifier.classify(item("TFT17_MarketOffering_RandomEmblem", "Random Emblem", List.of(), List.of()))).isEmpty();
	}

	@Test
	void shouldSkip_whenItemHasNoNameOrIsAugment() {
		assertThat(ItemClassifier.classify(item("TFT_Item_Blank", null, List.of(), List.of()))).isEmpty();
		assertThat(ItemClassifier.classify(new CdragonItem("DA_18_RiftbeastTraitAugment", "Omega Riftbeast",
				"icon.tex", List.of(), List.of(), true))).isEmpty();
	}

	@Test
	void shouldSkip_whenItemMatchesNoCategory() {
		CdragonItem item = item("DA_18_AfterShock", "Aftershock", List.of(), List.of("{5b609ae2}"));

		assertThat(ItemClassifier.classify(item)).isEmpty();
	}

	private static CdragonItem item(String apiName, String name, List<String> composition, List<String> tags) {
		return new CdragonItem(apiName, name, "icon.tex", composition, tags, false);
	}
}
