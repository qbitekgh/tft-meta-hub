package pl.kubit.tft_meta_hub.gamedata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.security.SecurityConfig;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(GamePatchAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class GamePatchAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GamePatchService gamePatchService;

	@Test
	void shouldAddPatchWithDateFromDatetimeLocalField_whenFormIsValid() throws Exception {
		mockMvc.perform(post("/admin/patches").with(csrf())
						.param("patch", "18.2")
						.param("setNumber", "18")
						.param("startsAt", "2026-11-19T10:00"))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(GamePatchAdminController.SUCCESS_MESSAGE, "Patch 18.2 added."));

		then(gamePatchService).should().add(argThat(form -> form.getPatch().equals("18.2")
				&& form.getSetNumber() == 18
				&& form.getStartsAt().equals(LocalDateTime.parse("2026-11-19T10:00"))));
	}

	@Test
	void shouldShowErrorsWithoutSaving_whenFieldsAreInvalid() throws Exception {
		mockMvc.perform(post("/admin/patches").with(csrf())
						.param("patch", "next")
						.param("setNumber", "18")
						.param("startsAt", "yesterday"))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(GamePatchAdminController.ERROR_MESSAGE, containsString("patch must look like 16.3")))
				.andExpect(flash().attribute(GamePatchAdminController.ERROR_MESSAGE, containsString("startsAt has an invalid value")));

		then(gamePatchService).should(never()).add(any());
	}

	@Test
	void shouldShowServiceMessage_whenPatchAlreadyExists() throws Exception {
		willThrow(new GamePatchException("Patch 18.2 already exists.")).given(gamePatchService).add(any());

		mockMvc.perform(post("/admin/patches").with(csrf())
						.param("patch", "18.2")
						.param("setNumber", "18")
						.param("startsAt", "2026-11-19T10:00"))
				.andExpect(flash().attribute(GamePatchAdminController.ERROR_MESSAGE, "Patch 18.2 already exists."));
	}
}
