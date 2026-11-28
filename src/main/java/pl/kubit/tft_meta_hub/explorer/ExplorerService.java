package pl.kubit.tft_meta_hub.explorer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.ActiveFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.FilterOptions;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.PlacementBar;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.Summary;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalog;
import pl.kubit.tft_meta_hub.gamedata.GameDataCatalogService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchView;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The explorer use case: statistics of collected boards for the given parameters, filters and tab.
 * Returns only aggregated numbers; no player is ever identifiable from them.
 */
@Service
@RequiredArgsConstructor
public class ExplorerService {

	private final ExplorerQueryRepository queryRepository;
	private final GamePatchService gamePatchService;
	private final GameDataCatalogService gameDataCatalogService;
	private final Clock clock;

	/** The validated state of a request, with the newest patch as the default. */
	public ExplorerState resolveState(ExplorerRequest request) {
		return ExplorerState.from(request, patchNames(gamePatchService.findAll()));
	}

	public ExplorerPage explore(ExplorerRequest request) {
		List<GamePatchView> patches = gamePatchService.findAll();
		List<String> patchNames = patchNames(patches);
		ExplorerState state = ExplorerState.from(request, patchNames);
		if (state.patch() == null) {
			return ExplorerPage.withoutData(state);
		}
		int setNumber = patches.stream()
				.filter(patch -> patch.patch().equals(state.patch()))
				.findFirst()
				.orElseThrow()
				.setNumber();
		GameDataCatalog catalog = gameDataCatalogService.getCatalog(setNumber);

		Instant since = clock.instant().minus(Duration.ofDays(state.days()));
		BoardSelection selection = new BoardSelection(state.patch(), state.server(), since, state.filters());
		PlacementDistribution placements = queryRepository.placements(selection);
		long allGames = state.filters().isEmpty() ? placements.games()
				: queryRepository.placements(selection.withoutFilters()).games();

		List<StatsRow> stats = switch (state.tab()) {
			case UNITS -> queryRepository.unitStats(selection, state.splitByStars());
			case ITEMS -> queryRepository.itemStats(selection, state.holder());
			// builds only make sense for one unit; the page asks to choose a holder first
			case BUILDS -> state.holder() == null ? List.of() : queryRepository.buildStats(selection, state.holder());
			case TRAITS -> queryRepository.traitStats(selection);
		};

		return new ExplorerPage(state,
				patchNames,
				summary(placements, allGames),
				state.filters().stream()
						.map(filter -> new ActiveFilter(filter, FilterLabels.label(filter, catalog)))
						.toList(),
				ExplorerRows.build(state, stats, catalog, placements.games()),
				new FilterOptions(catalog.championsByName(), catalog.boardItems(), catalog.traitsByName()));
	}

	private static List<String> patchNames(List<GamePatchView> patches) {
		return patches.stream().map(GamePatchView::patch).toList();
	}

	static Summary summary(PlacementDistribution placements, long allGames) {
		long games = placements.games();
		long mostCommon = placements.counts().stream().mapToLong(Long::longValue).max().orElse(0);
		List<PlacementBar> bars = IntStream.rangeClosed(1, PlacementDistribution.PLACEMENTS)
				.mapToObj(placement -> {
					long count = placements.gamesAt(placement);
					return new PlacementBar(placement, count,
							games == 0 ? 0 : (double) count / games,
							mostCommon == 0 ? 0 : 100.0 * count / mostCommon);
				})
				.toList();
		double share = allGames == 0 ? 0 : (double) games / allGames;
		return new Summary(placements.toStats(), allGames, share, bars);
	}
}
