package pl.kubit.tft_meta_hub.collector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "Collect now" in the admin panel: starts a run in the background and returns at once. */
@Controller
@RequiredArgsConstructor
class CollectorAdminController {

	static final String SUCCESS_MESSAGE = "successMessage";
	static final String ERROR_MESSAGE = "errorMessage";

	private final CollectorRunner collectorRunner;

	@PostMapping("/admin/collector/run")
	String collectNow(RedirectAttributes redirectAttributes) {
		if (collectorRunner.startInBackground(RunTrigger.MANUAL)) {
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
					"Collection started in the background. Refresh this page to see its progress.");
		} else {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "A collection run is already in progress.");
		}
		return "redirect:/admin";
	}
}
