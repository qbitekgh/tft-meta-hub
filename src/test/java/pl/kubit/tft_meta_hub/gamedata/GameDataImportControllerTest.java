package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import pl.kubit.tft_meta_hub.gamedata.GameDataImportResult.ImportCounts;
import pl.kubit.tft_meta_hub.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(GameDataImportController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class GameDataImportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameDataImportService importService;

	@Test
	void shouldImportSetAndShowSummary_whenSetNumberIsValid() throws Exception {
		given(importService.importSet(18)).willReturn(new GameDataImportResult(18,
				new ImportCounts(74, 0), new ImportCounts(36, 0), new ImportCounts(200, 32)));

		mockMvc.perform(post("/admin/game-data/import").param("setNumber", "18").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(GameDataImportController.SUCCESS_MESSAGE,
						"Set 18 imported. Champions: 74 added, 0 updated. Traits: 36 added, 0 updated. "
								+ "Items: 200 added, 32 updated."));
	}

	@Test
	void shouldShowError_whenSetDoesNotExist() throws Exception {
		given(importService.importSet(19)).willThrow(new SetNotFoundException(19));

		mockMvc.perform(post("/admin/game-data/import").param("setNumber", "19").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(GameDataImportController.ERROR_MESSAGE,
						"Set 19 was not found in Community Dragon data"));
	}

	@Test
	void shouldShowError_whenCommunityDragonIsUnavailable() throws Exception {
		given(importService.importSet(18)).willThrow(new ResourceAccessException("Connection timed out"));

		mockMvc.perform(post("/admin/game-data/import").param("setNumber", "18").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(GameDataImportController.ERROR_MESSAGE,
						"Could not download data from Community Dragon. Please try again later."));
	}

	@Test
	void shouldShowValidationErrorWithoutImporting_whenSetNumberIsInvalid() throws Exception {
		for (String invalidSetNumber : new String[]{"0", "100", "abc", ""}) {
			mockMvc.perform(post("/admin/game-data/import").param("setNumber", invalidSetNumber).with(csrf()))
					.andExpect(redirectedUrl("/admin"))
					.andExpect(flash().attribute(GameDataImportController.ERROR_MESSAGE,
							"Set number must be a whole number between 1 and 99."));
		}

		then(importService).should(never()).importSet(anyInt());
	}
}
