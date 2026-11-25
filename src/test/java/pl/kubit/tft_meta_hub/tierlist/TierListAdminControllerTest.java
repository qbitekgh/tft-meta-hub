package pl.kubit.tft_meta_hub.tierlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.security.SecurityConfig;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TierListAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class TierListAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TierListService tierListService;

	@Test
	void shouldCreateDraftAndRedirectToIt_whenFormIsValid() throws Exception {
		given(tierListService.createDraft(any())).willReturn(5L);

		mockMvc.perform(post("/admin/tierlists").with(csrf())
						.param("title", "Patch 18.1")
						.param("patch", "18.1")
						.param("setNumber", "18"))
				.andExpect(redirectedUrl("/admin/tierlists/5"));

		then(tierListService).should().createDraft(argThat(form ->
				form.getTitle().equals("Patch 18.1") && form.getPatch().equals("18.1") && form.getSetNumber() == 18));
	}

	@Test
	void shouldShowFieldErrorsWithoutSaving_whenPatchHasWrongFormat() throws Exception {
		mockMvc.perform(post("/admin/tierlists").with(csrf())
						.param("title", "Patch")
						.param("patch", "next week")
						.param("setNumber", "18"))
				.andExpect(status().isOk())
				.andExpect(view().name(TierListAdminController.FORM_VIEW))
				.andExpect(model().attributeHasFieldErrors("tierListForm", "patch"))
				.andExpect(content().string(containsString("must look like 16.3 or 16.3b")));

		then(tierListService).should(never()).createDraft(any());
	}

	@Test
	void shouldShowRuleMessageInForm_whenSetHasNoGameData() throws Exception {
		given(tierListService.createDraft(any()))
				.willThrow(new TierListRuleException("There is no game data for set 19. Import the set first."));

		mockMvc.perform(post("/admin/tierlists").with(csrf())
						.param("title", "Set 19")
						.param("patch", "19.1")
						.param("setNumber", "19"))
				.andExpect(status().isOk())
				.andExpect(view().name(TierListAdminController.FORM_VIEW))
				.andExpect(content().string(containsString("There is no game data for set 19. Import the set first.")));
	}

	@Test
	void shouldShowCompositionsAndActions_whenOpeningDraftDetails() throws Exception {
		given(tierListService.getDetails(5L)).willReturn(sampleDetails(TierListStatus.DRAFT, true, true));

		mockMvc.perform(get("/admin/tierlists/5"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Fae Xayah")))
				.andExpect(content().string(containsString("Guinsoo&#39;s Rageblade")))
				.andExpect(content().string(containsString("Publish")))
				.andExpect(content().string(containsString("Add composition")));
	}

	@Test
	void shouldHideEditActions_whenTierListIsArchived() throws Exception {
		given(tierListService.getDetails(5L)).willReturn(sampleDetails(TierListStatus.ARCHIVED, false, false));

		mockMvc.perform(get("/admin/tierlists/5"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("archived and read-only")))
				.andExpect(content().string(not(containsString("Add composition"))));
	}

	@Test
	void shouldReturn404_whenTierListDoesNotExist() throws Exception {
		given(tierListService.getDetails(99L)).willThrow(new TierListNotFoundException("Tier list 99 not found"));

		mockMvc.perform(get("/admin/tierlists/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldPublishAndShowMessage_whenPublishIsPosted() throws Exception {
		mockMvc.perform(post("/admin/tierlists/5/publish").with(csrf()))
				.andExpect(redirectedUrl("/admin/tierlists/5"))
				.andExpect(flash().attribute(TierListAdminController.SUCCESS_MESSAGE, "Tier list published."));

		then(tierListService).should().publish(5L);
	}

	@Test
	void shouldShowError_whenPublishingIsNotAllowed() throws Exception {
		willThrow(new TierListRuleException("Only a draft can be published.")).given(tierListService).publish(5L);

		mockMvc.perform(post("/admin/tierlists/5/publish").with(csrf()))
				.andExpect(redirectedUrl("/admin/tierlists/5"))
				.andExpect(flash().attribute(TierListAdminController.ERROR_MESSAGE, "Only a draft can be published."));
	}

	@Test
	void shouldCopyAndRedirectToNewDraft_whenCopyFormIsValid() throws Exception {
		given(tierListService.copyToNewPatch(any(Long.class), any())).willReturn(6L);

		mockMvc.perform(post("/admin/tierlists/5/copy").with(csrf())
						.param("title", "Patch 18.2")
						.param("patch", "18.2")
						.param("setNumber", "18"))
				.andExpect(redirectedUrl("/admin/tierlists/6"));
	}

	@Test
	void shouldRedirectToAdminPanel_whenDraftIsDeleted() throws Exception {
		mockMvc.perform(post("/admin/tierlists/5/delete").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(TierListAdminController.SUCCESS_MESSAGE, "Tier list deleted."));
	}

	private static TierListDetails sampleDetails(TierListStatus status, boolean editable, boolean deletable) {
		var guinsoo = new TierListDetails.ItemView(20L, "Guinsoo's Rageblade", null);
		var xayah = new TierListDetails.UnitView(10L, "Xayah", null, 1, true, 3, List.of(guinsoo));
		var composition = new TierListDetails.CompositionView(1L, "Fae Xayah", Tier.S, 1, PlayStyle.REROLL_1_COST,
				Difficulty.EASY, null, List.of(xayah));
		return new TierListDetails(5L, "Patch 18.1", "18.1", 18, status, null, Instant.parse("2026-11-25T12:00:00Z"),
				editable, deletable, List.of(composition));
	}
}
