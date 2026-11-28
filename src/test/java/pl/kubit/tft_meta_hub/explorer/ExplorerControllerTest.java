package pl.kubit.tft_meta_hub.explorer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.TraitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerFilter.UnitFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.ActiveFilter;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.ExplorerRow;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.FilterOptions;
import pl.kubit.tft_meta_hub.explorer.ExplorerPage.RowIcon;
import pl.kubit.tft_meta_hub.security.SecurityConfig;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.AHRI;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.BRAWLER;
import static pl.kubit.tft_meta_hub.explorer.ExplorerTestData.catalog;

@WebMvcTest(ExplorerController.class)
@Import(SecurityConfig.class)
class ExplorerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExplorerService explorerService;

	@Test
	void shouldShowEmptyState_whenThereIsNoPatch() throws Exception {
		given(explorerService.explore(any())).willReturn(ExplorerPage.withoutData(
				ExplorerState.from(ExplorerRequest.empty(), List.of())));

		mockMvc.perform(get("/explorer"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No match data yet.")))
				.andExpect(content().string(not(containsString("placement-chart"))));
	}

	@Test
	void shouldShowSummaryFiltersAndRows_whenDataExists() throws Exception {
		ExplorerState state = ExplorerState.from(ExplorerRequest.empty(), List.of("18.7"))
				.withFiltersAdded(List.of(new TraitFilter(BRAWLER, 4)));
		PlacementDistribution placements = new PlacementDistribution(List.of(3L, 1L, 0L, 0L, 0L, 0L, 0L, 0L));
		ExplorerRow ahri = new ExplorerRow("Ahri", List.of(new RowIcon("Ahri", "https://cdn.test/ahri.png", 4, 0)),
				new Stats(4, 1.25, 1.0, 0.75), 1.0, List.of(UnitFilter.of(AHRI)));
		given(explorerService.explore(any())).willReturn(new ExplorerPage(state, List.of("18.7"),
				ExplorerService.summary(placements, 40),
				List.of(new ActiveFilter(new TraitFilter(BRAWLER, 4), "Brawler 4+")),
				List.of(ahri),
				new FilterOptions(catalog().championsByName(), catalog().boardItems(), catalog().traitsByName())));

		mockMvc.perform(get("/explorer").param("f", "trait:DA_18_Brawler:4"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Brawler 4+")))
				.andExpect(content().string(containsString("1.25")))
				.andExpect(content().string(containsString("10.0% of 40")))
				.andExpect(content().string(containsString("unit:TFT18_Ahri:1-3:0-3")))
				.andExpect(content().string(containsString("src=\"https://cdn.test/ahri.png\"")))
				.andExpect(content().string(containsString("stat-icon--cost-4")))
				// removing the only filter leads back to all boards of the patch
				.andExpect(content().string(containsString("href=\"/explorer?patch=18.7\"")));
	}

	@Test
	void shouldRedirectToLinkWithNewFilter_whenAddFilterFormIsSent() throws Exception {
		given(explorerService.resolveState(any())).willReturn(ExplorerState.from(
				new ExplorerRequest("18.7", null, null, List.of("trait:DA_18_Brawler:4"), null, null, null, null, null, null),
				List.of("18.7")));

		mockMvc.perform(get("/explorer/add-filter")
						.param("patch", "18.7")
						.param("f", "trait:DA_18_Brawler:4")
						.param("type", "unit")
						.param("unit", AHRI)
						.param("minStars", "2")
						.param("maxStars", "3"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/explorer?patch=18.7&f=trait:DA_18_Brawler:4&f=unit:TFT18_Ahri:2-3:0-3"));
	}

	@Test
	void shouldRedirectWithoutChanges_whenAddFilterFormIsIncomplete() throws Exception {
		given(explorerService.resolveState(any())).willReturn(ExplorerState.from(ExplorerRequest.empty(), List.of("18.7")));

		mockMvc.perform(get("/explorer/add-filter").param("type", "unit").param("unit", ""))
				.andExpect(redirectedUrl("/explorer?patch=18.7"));
	}
}
