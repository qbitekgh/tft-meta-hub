package pl.kubit.tft_meta_hub.gamedata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the "Import set data" form of the admin panel. After the import it redirects back to the panel
 * (Post/Redirect/Get), passing the outcome as a flash attribute, so refreshing the page does not repeat the import.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
class GameDataImportController {

	static final String SUCCESS_MESSAGE = "successMessage";
	static final String ERROR_MESSAGE = "errorMessage";
	private static final String REDIRECT_TO_ADMIN = "redirect:/admin";

	private final GameDataImportService importService;

	@PostMapping("/admin/game-data/import")
	String importSet(@Valid @ModelAttribute ImportSetForm form,
					 BindingResult bindingResult,
					 RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Set number must be a whole number between 1 and 99.");
			return REDIRECT_TO_ADMIN;
		}
		try {
			GameDataImportResult result = importService.importSet(form.setNumber());
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, successMessage(result));
		} catch (SetNotFoundException e) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
		} catch (RestClientException e) {
			log.error("Game data import of set {} failed: Community Dragon request error", form.setNumber(), e);
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
					"Could not download data from Community Dragon. Please try again later.");
		}
		return REDIRECT_TO_ADMIN;
	}

	private static String successMessage(GameDataImportResult result) {
		return "Set %d imported. Champions: %s. Traits: %s. Items: %s.".formatted(
				result.setNumber(), describe(result.champions()), describe(result.traits()), describe(result.items()));
	}

	private static String describe(GameDataImportResult.ImportCounts counts) {
		return "%d added, %d updated".formatted(counts.added(), counts.updated());
	}

	record ImportSetForm(@NotNull @Min(1) @Max(99) Integer setNumber) {
	}
}
