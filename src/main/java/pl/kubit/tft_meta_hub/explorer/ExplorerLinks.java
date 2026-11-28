package pl.kubit.tft_meta_hub.explorer;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds explorer URLs from a state, used by the template for tabs, sorting, adding and removing filters.
 * The whole state lives in the URL (GET parameters), so every link can be shared and "back" works naturally.
 * Default values are left out to keep links short.
 * <p>
 * This is the only web-specific part of the explorer; the service knows nothing about URLs.
 */
public class ExplorerLinks {

	public static final String PATH = "/explorer";

	/** One URL parameter, also used for the hidden fields of the forms. */
	public record Param(String name, String value) {
	}

	public String url(ExplorerState state) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PATH);
		params(state).forEach(param -> builder.queryParam(param.name(), param.value()));
		return builder.encode().build().toUriString();
	}

	public List<Param> params(ExplorerState state) {
		List<Param> params = new ArrayList<>();
		add(params, "patch", state.patch());
		add(params, "server", state.server() == null ? null : state.server().name());
		if (state.days() != ExplorerState.DEFAULT_DAYS) {
			add(params, "days", String.valueOf(state.days()));
		}
		state.filters().forEach(filter -> add(params, "f", filter.toParam()));
		if (state.tab() != ExplorerTab.UNITS) {
			add(params, "tab", state.tab().name());
		}
		add(params, "holder", state.holder());
		if (state.splitByStars()) {
			add(params, "split", "true");
		}
		add(params, "cost", state.cost() == null ? null : String.valueOf(state.cost()));
		if (state.sort() != SortColumn.AVG_PLACE) {
			add(params, "sort", state.sort().name());
		}
		if (state.minGames() != ExplorerState.DEFAULT_MIN_GAMES) {
			add(params, "minGames", String.valueOf(state.minGames()));
		}
		return params;
	}

	/** Hidden fields for a form that shows some parameters as its own inputs. */
	public List<Param> paramsExcept(ExplorerState state, List<String> shownByForm) {
		return params(state).stream()
				.filter(param -> !shownByForm.contains(param.name()))
				.toList();
	}

	public List<Integer> dayOptions() {
		return ExplorerState.DAY_OPTIONS;
	}

	public List<ExplorerTab> tabs() {
		return List.of(ExplorerTab.values());
	}

	public String tab(ExplorerState state, String tab) {
		return url(state.withTab(ExplorerTab.valueOf(tab)));
	}

	public String sort(ExplorerState state, String column) {
		return url(state.withSort(SortColumn.valueOf(column)));
	}

	public String add(ExplorerState state, List<ExplorerFilter> filters) {
		return url(state.withFiltersAdded(filters));
	}

	public String remove(ExplorerState state, ExplorerFilter filter) {
		return url(state.withFilterRemoved(filter));
	}

	public String clearFilters(ExplorerState state) {
		return url(state.withoutFilters());
	}

	private static void add(List<Param> params, String name, String value) {
		if (value != null) {
			params.add(new Param(name, value));
		}
	}
}
