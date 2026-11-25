package pl.kubit.tft_meta_hub.collector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(CollectorAdminController.class)
@Import(SecurityConfig.class)
class CollectorAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CollectorRunner collectorRunner;

	@Test
	@WithMockUser(roles = "ADMIN")
	void shouldStartRunAndReturnImmediately_whenNothingIsRunning() throws Exception {
		given(collectorRunner.startInBackground(RunTrigger.MANUAL)).willReturn(true);

		mockMvc.perform(post("/admin/collector/run").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(CollectorAdminController.SUCCESS_MESSAGE,
						"Collection started in the background. Refresh this page to see its progress."));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void shouldShowMessage_whenRunIsAlreadyInProgress() throws Exception {
		given(collectorRunner.startInBackground(RunTrigger.MANUAL)).willReturn(false);

		mockMvc.perform(post("/admin/collector/run").with(csrf()))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute(CollectorAdminController.ERROR_MESSAGE, "A collection run is already in progress."));
	}

	@Test
	void shouldNotStartRun_whenUserIsAnonymous() throws Exception {
		mockMvc.perform(post("/admin/collector/run").with(csrf()))
				.andExpect(redirectedUrl("/login"));

		then(collectorRunner).should(never()).startInBackground(any());
	}
}
