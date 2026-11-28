package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerLinks.Param;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;

class ExplorerLinksTest {

	private final ExplorerLinks links = new ExplorerLinks();

	@Test
	void shouldLeaveOutDefaults_whenUrlIsBuilt() {
		ExplorerState state = ExplorerState.from(ExplorerRequest.empty(), List.of("18.7"));

		assertThat(links.url(state)).isEqualTo("/explorer?patch=18.7");
	}

	@Test
	void shouldWriteEveryChangedValue_whenUrlIsBuilt() {
		ExplorerRequest request = new ExplorerRequest("18.7", "EUNE", 3, List.of("trait:DA_18_Brawler:4"), "ITEMS",
				AHRI, true, 4, "WIN", 20);
		ExplorerState state = ExplorerState.from(request, List.of("18.7"));

		assertThat(links.url(state)).isEqualTo("/explorer?patch=18.7&server=EUNE&days=3&f=trait:DA_18_Brawler:4"
				+ "&tab=ITEMS&holder=TFT18_Ahri&split=true&cost=4&sort=WIN&minGames=20");
	}

	@Test
	void shouldKeepOtherParameters_whenTabOrFiltersChange() {
		ExplorerState state = ExplorerState.from(ExplorerRequest.empty(), List.of("18.7"));
		TraitFilter brawler = new TraitFilter(BRAWLER, 4);

		assertThat(links.tab(state, "TRAITS")).isEqualTo("/explorer?patch=18.7&tab=TRAITS");
		assertThat(links.add(state, List.of(brawler))).isEqualTo("/explorer?patch=18.7&f=trait:DA_18_Brawler:4");
		assertThat(links.remove(state.withFiltersAdded(List.of(brawler)), brawler)).isEqualTo("/explorer?patch=18.7");
	}

	@Test
	void shouldSkipParametersShownByForm_whenHiddenFieldsAreBuilt() {
		ExplorerState state = ExplorerState.from(ExplorerRequest.empty(), List.of("18.7")).withTab(ExplorerTab.ITEMS);

		assertThat(links.paramsExcept(state, List.of("patch"))).containsExactly(new Param("tab", "ITEMS"));
	}
}
