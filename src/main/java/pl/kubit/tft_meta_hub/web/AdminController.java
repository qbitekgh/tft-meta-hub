package pl.kubit.tft_meta_hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The admin panel; actions on it (e.g. game data import) are handled by controllers of their own features. */
@Controller
class AdminController {

	@GetMapping("/admin")
	String adminPanel() {
		return "admin/index";
	}
}
