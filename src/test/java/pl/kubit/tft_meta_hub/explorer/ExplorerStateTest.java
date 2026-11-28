package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.ItemFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.riot.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExplorerStateTest {

	private static final List<String> PATCHES = List.of("18.7", "18.6");

	@Test
	void shouldUseDefaults_whenRequestIsEmpty() {
		ExplorerState state = ExplorerState.from(ExplorerRequest.empty(), PATCHES);

		assertThat(state.patch()).isEqualTo("18.7");
		assertThat(state.server()).isNull();
		assertThat(state.days()).isEqualTo(ExplorerState.DEFAULT_DAYS);
		assertThat(state.filters()).isEmpty();
		assertThat(state.tab()).isEqualTo(ExplorerTab.UNITS);
		assertThat(state.holder()).isNull();
		assertThat(state.splitByStars()).isFalse();
		assertThat(state.cost()).isNull();
		assertThat(state.sort()).isEqualTo(SortColumn.AVG_PLACE);
		assertThat(state.minGames()).isEqualTo(ExplorerState.DEFAULT_MIN_GAMES);
	}

	@Test
	void shouldKeepValues_whenRequestIsValid() {
		ExplorerRequest request = new ExplorerRequest("18.6", "euw", 3, List.of("item:DA_18_EmblemFae"), "items",
				"TFT18_Ahri", true, 4, "games", 25);

		ExplorerState state = ExplorerState.from(request, PATCHES);

		assertThat(state).isEqualTo(new ExplorerState("18.6", Platform.EUW, 3, List.of(new ItemFilter("DA_18_EmblemFae")),
				ExplorerTab.ITEMS, "TFT18_Ahri", true, 4, SortColumn.GAMES, 25));
	}

	@Test
	void shouldReplaceInvalidValuesWithDefaults_whenRequestIsEdited() {
		ExplorerRequest request = new ExplorerRequest("99.9", "NA", 5, List.of("item:", "nonsense"), "comps",
				"<script>", null, 9, "name", -1);

		ExplorerState state = ExplorerState.from(request, PATCHES);

		assertThat(state).isEqualTo(ExplorerState.from(ExplorerRequest.empty(), PATCHES));
	}

	@Test
	void shouldHaveNoPatch_whenNoPatchIsKnown() {
		assertThat(ExplorerState.from(ExplorerRequest.empty(), List.of()).patch()).isNull();
	}

	@Test
	void shouldDropDuplicatesAndCapFilters_whenTooManyAreGiven() {
		List<String> params = IntStream.rangeClosed(1, 12).mapToObj(units -> "trait:DA_18_Brawler:" + units).toList();
		List<String> withDuplicate = new ArrayList<>(params);
		withDuplicate.addFirst("trait:DA_18_Brawler:1");

		ExplorerState state = ExplorerState.from(requestWithFilters(withDuplicate), PATCHES);

		assertThat(state.filters()).hasSize(ExplorerState.MAX_FILTERS);
		assertThat(state.filters()).first().isEqualTo(new TraitFilter("DA_18_Brawler", 1));
		assertThat(state.filters()).doesNotHaveDuplicates();
	}

	@Test
	void shouldAddAndRemoveFiltersWithoutChangingOtherValues_whenLinksAreBuilt() {
		ExplorerState state = ExplorerState.from(requestWithFilters(List.of("item:DA_18_EmblemFae")), PATCHES)
				.withTab(ExplorerTab.TRAITS);
		TraitFilter brawler = new TraitFilter("DA_18_Brawler", 4);

		ExplorerState added = state.withFiltersAdded(List.of(brawler, new ItemFilter("DA_18_EmblemFae")));
		ExplorerState removed = added.withFilterRemoved(new ItemFilter("DA_18_EmblemFae"));

		assertThat(added.filters()).containsExactly(new ItemFilter("DA_18_EmblemFae"), brawler);
		assertThat(removed.filters()).containsExactly(brawler);
		assertThat(removed.tab()).isEqualTo(ExplorerTab.TRAITS);
		assertThat(removed.withoutFilters().filters()).isEmpty();
	}

	private static ExplorerRequest requestWithFilters(List<String> filters) {
		return new ExplorerRequest(null, null, null, filters, null, null, null, null, null, null);
	}
}
