package pl.kubit.tft_meta_hub.gamedata.cdragon;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.gamedata.GameData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ChampionData;
import pl.kubit.tft_meta_hub.gamedata.GameData.ItemData;
import pl.kubit.tft_meta_hub.gamedata.GameData.TraitData;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.gamedata.SetNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class CdragonMapperTest {

	private final CdragonMapper mapper = new CdragonMapper(new CdragonIconUrls(CdragonFixtures.PROPERTIES));
	private final CdragonData sample = CdragonFixtures.sampleData();

	@Test
	void shouldMapOnlyPlayableChampions_whenSetContainsTechnicalUnits() {
		GameData gameData = mapper.map(sample, 18);

		assertThat(gameData.setNumber()).isEqualTo(18);
		assertThat(gameData.champions())
				.extracting(ChampionData::apiName)
				.containsExactly("DA_18_Xayah", "DA_18_Ornn", "DA_Lux18_Base", "DA_18_Lux_Fae");
	}

	@Test
	void shouldMapChampionFields_whenChampionIsPlayable() {
		GameData gameData = mapper.map(sample, 18);

		ChampionData luxFae = gameData.champions().stream()
				.filter(champion -> champion.apiName().equals("DA_18_Lux_Fae"))
				.findFirst()
				.orElseThrow();
		assertThat(luxFae.name()).isEqualTo("Lux (Fae)");
		assertThat(luxFae.cost()).isEqualTo(5);
		assertThat(luxFae.traitNames()).containsExactly("Fae", "Avatar");
		assertThat(luxFae.iconUrl())
				.isEqualTo("https://cdragon.test/latest/game/assets/characters/tft18_lux/tft18_lux_fae_square.png");
	}

	@Test
	void shouldMapTraitsWithSortedBreakpoints_whenEffectsHaveMinUnits() {
		GameData gameData = mapper.map(sample, 18);

		assertThat(gameData.traits())
				.extracting(TraitData::apiName, TraitData::name, TraitData::breakpoints)
				.containsExactlyInAnyOrder(
						tuple("DA_18_Elderwood", "Elderwood", List.of(3, 5, 7, 9, 11)),
						tuple("DA_18_LuxUniqueTrait", "Avatar", List.of(1)),
						tuple("DA_18_Rapidfire", "Rapidfire", List.of(2, 3, 4, 5)),
						tuple("DA_18_Fae", "Fae", List.of(2, 4)),
						tuple("DA_18_Defender", "Defender", List.of(2, 4, 6)),
						tuple("DA_18_Eclipse", "Eclipse", List.of()));
	}

	@Test
	void shouldKeepOnlyClassifiedItemsOfRankedMode_whenSetDataListsManyItems() {
		GameData gameData = mapper.map(sample, 18);

		assertThat(gameData.items())
				.extracting(ItemData::apiName, ItemData::category)
				.containsExactly(
						tuple("DA_Component_BFSword", ItemCategory.COMPONENT),
						tuple("DA_Component_RecurveBow", ItemCategory.COMPONENT),
						tuple("DA_Component_NeedlesslyLargeRod", ItemCategory.COMPONENT),
						tuple("DA_GuinsoosRageblade", ItemCategory.COMPLETED),
						tuple("TFT_Item_GuinsoosRageblade", ItemCategory.COMPLETED),
						tuple("DA_18_EmblemFae", ItemCategory.EMBLEM),
						tuple("DA_GuinsoosRagebladeRadiant", ItemCategory.RADIANT),
						tuple("TFT_Item_Artifact_Fishbones", ItemCategory.ARTIFACT));
	}

	@Test
	void shouldThrowSetNotFound_whenSetIsMissingInData() {
		assertThatThrownBy(() -> mapper.map(sample, 19))
				.isInstanceOf(SetNotFoundException.class)
				.hasMessageContaining("19");
	}
}
