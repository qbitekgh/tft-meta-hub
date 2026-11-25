package pl.kubit.tft_meta_hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

	@GetMapping("/")
	String home() {
		return "home";
	}
}
