package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ChampionInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ItemInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.TraitInfo;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;

import java.util.List;
import java.util.Map;

/** A tiny set of game data shared by the explorer's unit tests. */
final class ExplorerTestData {

	static final String AHRI = "TFT18_Ahri";
	static final String ORNN = "TFT18_Ornn";
	static final String KAYLE = "TFT18_Kayle";
	static final String GUINSOO = "DA_GuinsoosRageblade";
	static final String INFINITY_EDGE = "DA_InfinityEdge";
	static final String DEATHBLADE = "DA_Deathblade";
	static final String WARMOG = "DA_WarmogsArmor";
	static final String BRAWLER = "DA_18_Brawler";
	/** In match data but not in the game data, like the set 18 potions. */
	static final String POTION = "DA_BlastPotion18";

	private ExplorerTestData() {
	}

	static GameDataCatalog catalog() {
		return new GameDataCatalog(18,
				Map.of(AHRI, new ChampionInfo(AHRI, "Ahri", 4, "https://cdn.test/ahri.png"),
						ORNN, new ChampionInfo(ORNN, "Ornn", 1, null),
						KAYLE, new ChampionInfo(KAYLE, "Kayle", 5, null)),
				Map.of(BRAWLER, new TraitInfo(BRAWLER, "Brawler", null, List.of(2, 4, 6))),
				Map.of(GUINSOO, item(GUINSOO, "Guinsoo's Rageblade"),
						INFINITY_EDGE, item(INFINITY_EDGE, "Infinity Edge"),
						DEATHBLADE, item(DEATHBLADE, "Deathblade"),
						WARMOG, item(WARMOG, "Warmog's Armor")));
	}

	static ExplorerState state(ExplorerTab tab) {
		return ExplorerState.from(ExplorerRequest.empty(), List.of("18.7")).withTab(tab);
	}

	static StatsRow row(String key, int level, long games, double avgPlacement) {
		return new StatsRow(key, level, new Stats(games, avgPlacement, 0.5, 0.1));
	}

	private static ItemInfo item(String apiName, String name) {
		return new ItemInfo(apiName, name, null, ItemCategory.COMPLETED);
	}
}
