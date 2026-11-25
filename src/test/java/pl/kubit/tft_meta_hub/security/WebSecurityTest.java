package pl.kubit.tft_meta_hub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.tft_meta_hub.collector.CollectorRunner;
import pl.kubit.tft_meta_hub.collector.CollectorStatus;
import pl.kubit.tft_meta_hub.collector.CollectorStatusService;
import pl.kubit.tft_meta_hub.gamedata.GameDataImportService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.tierlist.PublicTierListService;
import pl.kubit.tft_meta_hub.tierlist.TierListService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web layer with the real security configuration (admin credentials from src/test/resources/config/application.yaml).
 */
@WebMvcTest
@Import(SecurityConfig.class)
class WebSecurityTest {

	private static final String RIOT_NOTICE = "TFT Meta Hub isn't endorsed by Riot Games";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameDataImportService gameDataImportService;

	@MockitoBean
	private TierListService tierListService;

	@MockitoBean
	private PublicTierListService publicTierListService;

	@MockitoBean
	private CollectorRunner collectorRunner;

	@MockitoBean
	private CollectorStatusService collectorStatusService;

	@MockitoBean
	private GamePatchService gamePatchService;

	@BeforeEach
	void setUp() {
		given(collectorStatusService.getStatus()).willReturn(new CollectorStatus(false, null, 0, null));
	}

	@Test
	void shouldShowHomePageWithRiotNotice_whenUserIsAnonymous() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(content().string(containsString("No tier list has been published yet.")))
				.andExpect(content().string(containsString(RIOT_NOTICE)))
				.andExpect(content().string(containsString("Admin login")));
	}

	@Test
	void shouldShowLoginPage_whenUserIsAnonymous() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(content().string(containsString(RIOT_NOTICE)));
	}

	@Test
	void shouldRedirectToLogin_whenAnonymousUserOpensAdminPanel() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void shouldRedirectToLogin_whenAnonymousUserOpensAdminSubpage() throws Exception {
		mockMvc.perform(get("/admin/tierlists"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void shouldShowAdminPanel_whenUserIsAdmin() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/index"))
				.andExpect(content().string(containsString("Import set data")))
				.andExpect(content().string(containsString("Log out")))
				.andExpect(content().string(not(containsString("Admin login"))));
	}

	@Test
	@WithMockUser(roles = "USER")
	void shouldForbidAdminPanel_whenUserIsNotAdmin() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldAuthenticateAndRedirectToAdmin_whenAdminCredentialsAreCorrect() throws Exception {
		mockMvc.perform(formLogin("/login").user("test-admin").password("test-password"))
				.andExpect(authenticated().withRoles("ADMIN"))
				.andExpect(redirectedUrl("/admin"));
	}

	@Test
	void shouldRejectLogin_whenPasswordIsWrong() throws Exception {
		mockMvc.perform(formLogin("/login").user("test-admin").password("wrong-password"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void shouldRejectLogin_whenCsrfTokenIsMissing() throws Exception {
		mockMvc.perform(post("/login").param("username", "test-admin").param("password", "test-password"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void shouldRedirectToHome_whenUserLogsOut() throws Exception {
		mockMvc.perform(logout())
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void shouldRejectAdminPost_whenCsrfTokenIsMissing() throws Exception {
		mockMvc.perform(post("/admin/game-data/import").param("setNumber", "18"))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldServeStylesheet_whenUserIsAnonymous() throws Exception {
		mockMvc.perform(get("/css/style.css"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldNotAllowAnonymousImport_whenCsrfTokenIsPresent() throws Exception {
		mockMvc.perform(post("/admin/game-data/import").param("setNumber", "18").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}
}
