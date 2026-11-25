package pl.kubit.tft_meta_hub.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.kubit.tft_meta_hub.collector.CollectorStatusService;
import pl.kubit.tft_meta_hub.gamedata.GamePatchService;
import pl.kubit.tft_meta_hub.tierlist.TierListService;

/** The admin panel; actions on it (e.g. game data import) are handled by controllers of their own features. */
@Controller
@RequiredArgsConstructor
class AdminController {

	private final TierListService tierListService;
	private final CollectorStatusService collectorStatusService;
	private final GamePatchService gamePatchService;

	@GetMapping("/admin")
	String adminPanel(Model model) {
		model.addAttribute("tierLists", tierListService.findAll());
		model.addAttribute("collector", collectorStatusService.getStatus());
		model.addAttribute("patches", gamePatchService.findAll());
		return "admin/index";
	}
}
