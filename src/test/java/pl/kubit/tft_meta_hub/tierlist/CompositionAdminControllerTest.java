package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.gamedata.ItemCategory;
import pl.kubit.tft_meta_hub.security.SecurityConfig;
import pl.kubit.tft_meta_hub.tierlist.CompositionForm.UnitForm;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ChampionOption;
import pl.kubit.tft_meta_hub.tierlist.CompositionFormOptions.ItemOption;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CompositionAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class CompositionAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TierListService tierListService;

	@BeforeEach
	void setUp() {
		Map<ItemCategory, List<ItemOption>> items = new EnumMap<>(ItemCategory.class);
		items.put(ItemCategory.COMPLETED, List.of(new ItemOption(20L, "Guinsoo's Rageblade")));
		given(tierListService.getCompositionFormOptions(5L)).willReturn(new CompositionFormOptions(5L, "Patch 18.1", 18,
				List.of(new ChampionOption(10L, "Xayah", 1), new ChampionOption(11L, "Ornn", 1)), items));
	}

	@Test
	void shouldRenderTenEmptyUnitRows_whenOpeningNewCompositionForm() throws Exception {
		mockMvc.perform(get("/admin/tierlists/5/compositions/new"))
				.andExpect(status().isOk())
				.andExpect(view().name(CompositionAdminController.FORM_VIEW))
				.andExpect(content().string(containsString("name=\"units[9].championId\"")))
				.andExpect(content().string(containsString("name=\"units[9].itemIds[2]\"")))
				.andExpect(content().string(containsString("Xayah (1)")))
				.andExpect(content().string(containsString("<optgroup label=\"COMPLETED\">")));
	}

	@Test
	void shouldBindUnitRowsAndRedirect_whenFormIsValid() throws Exception {
		mockMvc.perform(post("/admin/tierlists/5/compositions").with(csrf())
						.param("name", "Fae Xayah")
						.param("tier", "S")
						.param("position", "1")
						.param("playStyle", "REROLL_1_COST")
						.param("difficulty", "EASY")
						.param("tips", "Roll at level 5.")
						.param("units[0].championId", "10")
						.param("units[0].starLevel", "3")
						.param("units[0].carry", "true")
						.param("units[0].itemIds[0]", "20")
						.param("units[0].itemIds[1]", "")
						.param("units[0].itemIds[2]", "20")
						.param("units[1].championId", "")
						.param("units[1].starLevel", "2"))
				.andExpect(redirectedUrl("/admin/tierlists/5"));

		ArgumentCaptor<CompositionForm> captor = ArgumentCaptor.forClass(CompositionForm.class);
		then(tierListService).should().addComposition(eq(5L), captor.capture());
		CompositionForm form = captor.getValue();
		assertThat(form.getTier()).isEqualTo(Tier.S);
		assertThat(form.selectedUnits()).hasSize(1);
		UnitForm unit = form.selectedUnits().getFirst();
		assertThat(unit.getChampionId()).isEqualTo(10L);
		assertThat(unit.isCarry()).isTrue();
		assertThat(unit.getStarLevel()).isEqualTo(3);
		assertThat(unit.selectedItemIds()).containsExactly(20L, 20L);
	}

	@Test
	void shouldRerenderFormWithErrors_whenRequiredFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/admin/tierlists/5/compositions").with(csrf())
						.param("name", "")
						.param("position", "1")
						.param("units[0].championId", "10")
						.param("units[0].starLevel", "2"))
				.andExpect(status().isOk())
				.andExpect(view().name(CompositionAdminController.FORM_VIEW))
				.andExpect(model().attributeHasFieldErrors("compositionForm", "name", "tier", "playStyle", "difficulty"))
				.andExpect(content().string(containsString("name=\"units[9].championId\"")));

		then(tierListService).should(never()).addComposition(anyLong(), any());
	}

	@Test
	void shouldShowRuleMessage_whenDomainRejectsComposition() throws Exception {
		willThrow(new TierListRuleException("A composition must have between 1 and 10 units, but has 0."))
				.given(tierListService).addComposition(eq(5L), any());

		mockMvc.perform(post("/admin/tierlists/5/compositions").with(csrf())
						.param("name", "Empty")
						.param("tier", "A")
						.param("position", "2")
						.param("playStyle", "FLEX")
						.param("difficulty", "HARD"))
				.andExpect(status().isOk())
				.andExpect(view().name(CompositionAdminController.FORM_VIEW))
				.andExpect(content().string(containsString("A composition must have between 1 and 10 units, but has 0.")));
	}

	@Test
	void shouldPrefillForm_whenEditingExistingComposition() throws Exception {
		var guinsoo = new TierListDetails.ItemView(20L, "Guinsoo's Rageblade", null);
		var xayah = new TierListDetails.UnitView(10L, "Xayah", null, 1, true, 3, List.of(guinsoo));
		var composition = new TierListDetails.CompositionView(1L, "Fae Xayah", Tier.S, 1, PlayStyle.REROLL_1_COST,
				Difficulty.EASY, "Tips", List.of(xayah));
		given(tierListService.getDetails(5L)).willReturn(new TierListDetails(5L, "Patch 18.1", "18.1", 18,
				TierListStatus.DRAFT, null, Instant.parse("2026-11-25T12:00:00Z"), true, true, List.of(composition)));

		mockMvc.perform(get("/admin/tierlists/5/compositions/1/edit"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"Fae Xayah\"")))
				.andExpect(content().string(containsString("<option value=\"10\" selected=\"selected\">Xayah (1)</option>")));
	}
}
