package pl.kubit.tft_meta_hub.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Shows the login form; the POST to {@code /login} is handled by Spring Security itself. */
@Controller
class LoginController {

	@GetMapping("/login")
	String login() {
		return "login";
	}
}
