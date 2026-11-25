package pl.kubit.tft_meta_hub.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.kubit.tft_meta_hub.tierlist.PublicTierListService;

@Controller
@RequiredArgsConstructor
class HomeController {

	private final PublicTierListService publicTierListService;

	/** The home page is the published tier list; {@code tierList} is null when nothing is published yet. */
	@GetMapping("/")
	String home(Model model) {
		model.addAttribute("tierList", publicTierListService.findPublished().orElse(null));
		return "home";
	}
}
