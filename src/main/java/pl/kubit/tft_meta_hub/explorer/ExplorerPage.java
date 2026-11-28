package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ChampionInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.ItemInfo;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog.TraitInfo;

import java.util.List;

/**
 * Everything the explorer page shows. Plain records, so a future REST API can return the same object as JSON.
 *
 * @param patches patches to choose from, newest first; empty when no patch has been entered yet
 * @param summary statistics of all selected boards; null when there is no patch yet
 * @param rows    rows of the selected tab, already filtered and sorted
 */
public record ExplorerPage(ExplorerState state,
						   List<String> patches,
						   Summary summary,
						   List<ActiveFilter> filters,
						   List<ExplorerRow> rows,
						   FilterOptions options) {

	static ExplorerPage withoutData(ExplorerState state) {
		return new ExplorerPage(state, List.of(), null, List.of(), List.of(),
				new FilterOptions(List.of(), List.of(), List.of()));
	}

	public boolean hasPatch() {
		return state.patch() != null;
	}

	/**
	 * @param allGames boards of the general parameters without filters
	 * @param share    {@code stats.games / allGames}, 0.0–1.0
	 */
	public record Summary(Stats stats, long allGames, double share, List<PlacementBar> placements) {
	}

	/**
	 * @param share     share of the selected boards with this placement, 0.0–1.0
	 * @param barHeight height of the chart bar in percent; the most common placement is 100
	 */
	public record PlacementBar(int placement, long games, double share, double barHeight) {
	}

	public record ActiveFilter(ExplorerFilter filter, String label) {
	}

	/**
	 * @param frequency    share of the selected boards that contain this row's element, 0.0–1.0
	 * @param filtersToAdd filters that clicking the row adds (three for a build)
	 */
	public record ExplorerRow(String name, List<RowIcon> icons, Stats stats, double frequency,
							  List<ExplorerFilter> filtersToAdd) {
	}

	/**
	 * @param cost  champion cost for the coloured frame, or null for items and traits
	 * @param stars star level to show, or 0 for none
	 */
	public record RowIcon(String name, String iconUrl, Integer cost, int stars) {
	}

	/** Choices for the "add filter" forms and the item holder list. */
	public record FilterOptions(List<ChampionInfo> champions, List<ItemInfo> items, List<TraitInfo> traits) {
	}
}
