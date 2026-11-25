package pl.kubit.tft_meta_hub.tierlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
class PublicCompositionController {

	private final PublicTierListService publicTierListService;

	@GetMapping("/comps/{id}")
	String composition(@PathVariable long id, Model model) {
		model.addAttribute("published", publicTierListService.getComposition(id));
		return "comps/details";
	}
}
