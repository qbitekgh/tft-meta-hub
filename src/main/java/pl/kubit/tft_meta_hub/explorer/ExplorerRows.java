package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.ExplorerRow;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.RowIcon;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ChampionInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ItemInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.TraitInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turns raw statistics rows of a tab into table rows: names and icons from game data, the minimum number of games,
 * the cost filter and sorting. Elements missing from the game data (e.g. the set 18 potions) are left out.
 */
final class ExplorerRows {

	private ExplorerRows() {
	}

	/**
	 * @param selectedGames number of boards that pass the filters; the frequency of a row is a share of it
	 */
	static List<ExplorerRow> build(ExplorerState state, List<StatsRow> rows, GameDataCatalog catalog, long selectedGames) {
		Comparator<ExplorerRow> order = Comparator.comparing(ExplorerRow::stats, state.sort().comparator())
				.thenComparing(ExplorerRow::name);
		return rows.stream()
				.filter(row -> row.stats().games() >= state.minGames())
				.map(row -> toRow(state, row, catalog, selectedGames))
				.flatMap(Optional::stream)
				.sorted(order)
				.toList();
	}

	private static Optional<ExplorerRow> toRow(ExplorerState state, StatsRow row, GameDataCatalog catalog,
											   long selectedGames) {
		double frequency = selectedGames == 0 ? 0 : (double) row.stats().games() / selectedGames;
		return switch (state.tab()) {
			case UNITS -> catalog.champion(row.key())
					.filter(champion -> state.cost() == null || champion.cost() == state.cost())
					.map(champion -> unitRow(champion, row, frequency));
			case ITEMS -> catalog.item(row.key())
					.map(item -> itemRow(item, state.holder(), row, frequency));
			case BUILDS -> buildRow(row, state.holder(), catalog, frequency);
			case TRAITS -> catalog.trait(row.key())
					.map(trait -> traitRow(trait, row, frequency));
		};
	}

	/** {@code row.level()} is the star level when the table is split by stars, otherwise 0. */
	private static ExplorerRow unitRow(ChampionInfo champion, StatsRow row, double frequency) {
		int stars = row.level();
		UnitFilter filter = stars == 0 ? UnitFilter.of(champion.apiName())
				: new UnitFilter(champion.apiName(), Math.min(stars, ExplorerFilter.MAX_STARS),
				Math.min(stars, ExplorerFilter.MAX_STARS), 0, ExplorerFilter.MAX_ITEMS);
		String name = stars == 0 ? champion.name() : champion.name() + " " + stars + "★";
		RowIcon icon = new RowIcon(champion.name(), champion.iconUrl(), champion.cost(), stars);
		return new ExplorerRow(name, List.of(icon), row.stats(), frequency, List.of(filter));
	}

	private static ExplorerRow itemRow(ItemInfo item, String holder, StatsRow row, double frequency) {
		ExplorerFilter filter = holder == null ? new ItemFilter(item.apiName()) : new UnitItemFilter(holder, item.apiName());
		return new ExplorerRow(item.name(), List.of(itemIcon(item)), row.stats(), frequency, List.of(filter));
	}

	/** A build is left out when one of its items is not in the game data. */
	private static Optional<ExplorerRow> buildRow(StatsRow row, String holder, GameDataCatalog catalog, double frequency) {
		List<Optional<ItemInfo>> found = Arrays.stream(row.key().split(","))
				.map(catalog::item)
				.toList();
		if (holder == null || found.stream().anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		List<ItemInfo> items = found.stream().map(Optional::orElseThrow).toList();
		String name = String.join(", ", items.stream().map(ItemInfo::name).toList());
		List<RowIcon> icons = items.stream().map(ExplorerRows::itemIcon).toList();
		List<ExplorerFilter> filters = items.stream()
				.map(item -> (ExplorerFilter) new UnitItemFilter(holder, item.apiName()))
				.toList();
		return Optional.of(new ExplorerRow(name, icons, row.stats(), frequency, filters));
	}

	/**
	 * {@code row.level()} is Riot's trait level (1 = first breakpoint); the name shows the units it needs,
	 * e.g. "4 Brawler", and clicking adds "Brawler 4+".
	 */
	private static ExplorerRow traitRow(TraitInfo trait, StatsRow row, double frequency) {
		Optional<Integer> units = trait.unitsForLevel(row.level());
		String name = units.map(count -> count + " " + trait.name())
				.orElse(trait.name() + " (level " + row.level() + ")");
		RowIcon icon = new RowIcon(trait.name(), trait.iconUrl(), null, 0);
		TraitFilter filter = new TraitFilter(trait.apiName(), units.orElse(1));
		return new ExplorerRow(name, List.of(icon), row.stats(), frequency, List.of(filter));
	}

	private static RowIcon itemIcon(ItemInfo item) {
		return new RowIcon(item.name(), item.iconUrl(), null, 0);
	}
}
