package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.security.SecurityConfig;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.ItemView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.UnitView;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PublicCompositionController.class)
@Import(SecurityConfig.class)
class PublicCompositionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PublicTierListService publicTierListService;

	@Test
	void shouldShowBoardItemsAndTips_whenCompositionIsPublished() throws Exception {
		ItemView guinsoo = new ItemView(20L, "Guinsoo's Rageblade", "https://cdn.test/guinsoo.png");
		UnitView xayah = new UnitView(10L, "Xayah", "https://cdn.test/xayah.png", 1, true, 3, List.of(guinsoo));
		UnitView ornn = new UnitView(11L, "Ornn", null, 4, false, 2, List.of());
		CompositionView composition = new CompositionView(1L, "Fae Xayah", Tier.S, 1, PlayStyle.REROLL_1_COST,
				Difficulty.EASY, "Roll at level 5.\nSlam Guinsoo early.", List.of(xayah, ornn));
		given(publicTierListService.getComposition(1L))
				.willReturn(new PublishedComposition("Patch 18.1 meta", "18.1", composition));

		mockMvc.perform(get("/comps/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("comps/details"))
				.andExpect(content().string(containsString("Fae Xayah")))
				.andExpect(content().string(containsString("S tier")))
				.andExpect(content().string(containsString("1-cost reroll")))
				.andExpect(content().string(containsString("unit unit--large unit--cost-4")))
				.andExpect(content().string(containsString("src=\"https://cdn.test/xayah.png\"")))
				.andExpect(content().string(containsString("★★★")))
				.andExpect(content().string(containsString("Roll at level 5.")))
				.andExpect(content().string(containsString("Guinsoo&#39;s Rageblade")));
	}

	@Test
	void shouldReturn404_whenCompositionIsNotPublished() throws Exception {
		given(publicTierListService.getComposition(2L))
				.willThrow(new TierListNotFoundException("Composition 2 is not published"));

		mockMvc.perform(get("/comps/2"))
				.andExpect(status().isNotFound());
	}
}
