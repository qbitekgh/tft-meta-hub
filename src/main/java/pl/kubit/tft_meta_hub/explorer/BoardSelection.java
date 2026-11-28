package pl.kubit.tft_meta_hub.explorer;

import pl.kubit.tft_meta_hub.riot.Platform;

import java.time.Instant;
import java.util.List;

/**
 * Which boards a statistics query looks at: the general parameters plus the filters.
 *
 * @param platform the server, or null for both servers
 * @param since    only matches played at or after this moment
 */
public record BoardSelection(String patch, Platform platform, Instant since, List<ExplorerFilter> filters) {

	public BoardSelection {
		filters = List.copyOf(filters);
	}

	/** The same general parameters without filters: all boards the filtered ones are a share of. */
	public BoardSelection withoutFilters() {
		return new BoardSelection(patch, platform, since, List.of());
	}
}
