package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.riot.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Everything that decides what the explorer shows, validated and with defaults filled in.
 * It is immutable: the {@code with...} methods return the state of a link (another tab, one more filter, ...).
 *
 * @param patch        the patch, or null when no patch is known yet
 * @param server       the server, or null for both servers
 * @param holder       api name of the unit whose items the Items and Builds tabs show, or null for any unit
 * @param splitByStars Units tab: one row per champion and star level
 * @param cost         Units tab: only champions of this cost, or null for every cost
 */
public record ExplorerState(String patch,
							Platform server,
							int days,
							List<ExplorerFilter> filters,
							ExplorerTab tab,
							String holder,
							boolean splitByStars,
							Integer cost,
							SortColumn sort,
							int minGames) {

	public static final List<Integer> DAY_OPTIONS = List.of(1, 3, 7, 14);
	public static final int DEFAULT_DAYS = 14;
	public static final int DEFAULT_MIN_GAMES = 10;
	public static final int MAX_FILTERS = 8;

	public ExplorerState {
		filters = List.copyOf(filters);
	}

	/**
	 * @param knownPatches patches the admin entered, newest first; the newest one is the default
	 */
	public static ExplorerState from(ExplorerRequest request, List<String> knownPatches) {
		// List.of(...).contains(null) throws, so missing values are checked first
		String patch = request.patch() != null && knownPatches.contains(request.patch()) ? request.patch()
				: knownPatches.stream().findFirst().orElse(null);
		int days = request.days() != null && DAY_OPTIONS.contains(request.days()) ? request.days() : DEFAULT_DAYS;
		String holder = request.holder() != null && ExplorerFilter.API_NAME.matcher(request.holder()).matches()
				? request.holder() : null;
		Integer cost = request.cost() != null && request.cost() >= 1 && request.cost() <= 5 ? request.cost() : null;
		int minGames = request.minGames() != null && request.minGames() >= 0 ? request.minGames() : DEFAULT_MIN_GAMES;
		return new ExplorerState(patch,
				parseEnum(Platform.class, request.server()).orElse(null),
				days,
				parseFilters(request.f()),
				parseEnum(ExplorerTab.class, request.tab()).orElse(ExplorerTab.UNITS),
				holder,
				Boolean.TRUE.equals(request.split()),
				cost,
				parseEnum(SortColumn.class, request.sort()).orElse(SortColumn.AVG_PLACE),
				minGames);
	}

	public ExplorerState withFiltersAdded(List<ExplorerFilter> added) {
		List<ExplorerFilter> combined = new ArrayList<>(filters);
		combined.addAll(added);
		return withFilters(distinctAndLimited(combined));
	}

	public ExplorerState withFilterRemoved(ExplorerFilter removed) {
		return withFilters(filters.stream().filter(filter -> !filter.equals(removed)).toList());
	}

	public ExplorerState withoutFilters() {
		return withFilters(List.of());
	}

	public ExplorerState withTab(ExplorerTab newTab) {
		return new ExplorerState(patch, server, days, filters, newTab, holder, splitByStars, cost, sort, minGames);
	}

	public ExplorerState withSort(SortColumn newSort) {
		return new ExplorerState(patch, server, days, filters, tab, holder, splitByStars, cost, newSort, minGames);
	}

	private ExplorerState withFilters(List<ExplorerFilter> newFilters) {
		return new ExplorerState(patch, server, days, newFilters, tab, holder, splitByStars, cost, sort, minGames);
	}

	/** Invalid values are skipped, duplicates removed and the number of filters is capped. */
	private static List<ExplorerFilter> parseFilters(List<String> params) {
		if (params == null) {
			return List.of();
		}
		return distinctAndLimited(params.stream()
				.map(ExplorerFilter::parse)
				.flatMap(Optional::stream)
				.toList());
	}

	private static List<ExplorerFilter> distinctAndLimited(List<ExplorerFilter> filters) {
		return new LinkedHashSet<>(filters).stream().limit(MAX_FILTERS).toList();
	}

	private static <E extends Enum<E>> Optional<E> parseEnum(Class<E> type, String value) {
		if (value == null) {
			return Optional.empty();
		}
		String name = value.strip().toUpperCase(Locale.ROOT);
		return Arrays.stream(type.getEnumConstants())
				.filter(constant -> constant.name().equals(name))
				.findFirst();
	}
}
