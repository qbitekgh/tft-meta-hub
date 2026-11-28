package pl.kubit.tft_meta_hub.gamedata;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only game data of one set, looked up by api name (the identifier Riot match data uses).
 * Built once per request, so turning hundreds of statistics rows into names and icons needs no extra queries.
 */
public record GameDataCatalog(int setNumber,
							  Map<String, ChampionInfo> champions,
							  Map<String, TraitInfo> traits,
							  Map<String, ItemInfo> items) {

	public record ChampionInfo(String apiName, String name, int cost, String iconUrl) {
	}

	/** @param breakpoints minimum unit counts of the trait levels, e.g. [2, 4, 6] */
	public record TraitInfo(String apiName, String name, String iconUrl, List<Integer> breakpoints) {

		/**
		 * Units needed for a trait level as Riot numbers it ({@code tier_current}, starting at 1),
		 * e.g. level 2 of [2, 4, 6] needs 4 units.
		 */
		public Optional<Integer> unitsForLevel(int level) {
			if (level < 1 || level > breakpoints.size()) {
				return Optional.empty();
			}
			return Optional.of(breakpoints.get(level - 1));
		}
	}

	public record ItemInfo(String apiName, String name, String iconUrl, ItemCategory category) {
	}

	public GameDataCatalog {
		champions = Map.copyOf(champions);
		traits = Map.copyOf(traits);
		items = Map.copyOf(items);
	}

	public Optional<ChampionInfo> champion(String apiName) {
		return Optional.ofNullable(champions.get(apiName));
	}

	public Optional<TraitInfo> trait(String apiName) {
		return Optional.ofNullable(traits.get(apiName));
	}

	public Optional<ItemInfo> item(String apiName) {
		return Optional.ofNullable(items.get(apiName));
	}

	public List<ChampionInfo> championsByName() {
		return champions.values().stream()
				.sorted(Comparator.comparing(ChampionInfo::name))
				.toList();
	}

	public List<TraitInfo> traitsByName() {
		return traits.values().stream()
				.sorted(Comparator.comparing(TraitInfo::name))
				.toList();
	}

	/** Items that end up on a final board (no components), grouped by category order and then by name. */
	public List<ItemInfo> boardItems() {
		return items.values().stream()
				.filter(item -> item.category() != ItemCategory.COMPONENT)
				.sorted(Comparator.comparing(ItemInfo::category).thenComparing(ItemInfo::name))
				.toList();
	}
}
