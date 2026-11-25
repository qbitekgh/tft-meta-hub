package pl.kubit.tft_meta_hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.security.SecurityConfig;
import pl.kubit.tft_meta_hub.tierlist.Difficulty;
import pl.kubit.tft_meta_hub.tierlist.PlayStyle;
import pl.kubit.tft_meta_hub.tierlist.PublicTierListService;
import pl.kubit.tft_meta_hub.tierlist.PublishedTierList;
import pl.kubit.tft_meta_hub.tierlist.Tier;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.ItemView;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.UnitView;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PublicTierListService publicTierListService;

	@Test
	void shouldShowEmptyState_whenNothingIsPublished() throws Exception {
		given(publicTierListService.findPublished()).willReturn(Optional.empty());

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No tier list has been published yet.")));
	}

	@Test
	void shouldShowTierRowsAndCompositionCards_whenListIsPublished() throws Exception {
		ItemView guinsoo = new ItemView(20L, "Guinsoo's Rageblade", "https://cdn.test/guinsoo.png");
		UnitView carry = new UnitView(10L, "Xayah", "https://cdn.test/xayah.png", 1, true, 3, List.of(guinsoo));
		UnitView tank = new UnitView(11L, "Ornn", "https://cdn.test/ornn.png", 4, false, 2,
				List.of(new ItemView(21L, "Warmog's Armor", "https://cdn.test/warmog.png")));
		Map<Tier, List<CompositionView>> byTier = new EnumMap<>(Tier.class);
		byTier.put(Tier.S, List.of(new CompositionView(7L, "Fae Xayah", Tier.S, 1, PlayStyle.REROLL_1_COST,
				Difficulty.EASY, null, List.of(carry, tank))));
		byTier.put(Tier.SITUATIONAL, List.of(new CompositionView(8L, "Inferno Flex", Tier.SITUATIONAL, 1, PlayStyle.FLEX,
				Difficulty.HARD, null, List.of(tank))));
		given(publicTierListService.findPublished()).willReturn(Optional.of(new PublishedTierList(
				"Patch 18.1 meta", "18.1", 18, Instant.parse("2026-11-25T12:00:00Z"), byTier)));

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Patch 18.1 meta")))
				.andExpect(content().string(containsString("aria-label=\"S tier\"")))
				.andExpect(content().string(containsString("aria-label=\"Situational tier\"")))
				.andExpect(content().string(not(containsString("aria-label=\"A tier\""))))
				.andExpect(content().string(containsString("href=\"/comps/7\"")))
				.andExpect(content().string(containsString("unit unit--small unit--cost-1 unit--carry")))
				.andExpect(content().string(containsString("src=\"https://cdn.test/guinsoo.png\"")))
				// cards show items only on carries, so the tank's item is not rendered
				.andExpect(content().string(not(containsString("warmog.png"))))
				.andExpect(content().string(not(containsString("No tier list has been published yet."))));
	}
}
