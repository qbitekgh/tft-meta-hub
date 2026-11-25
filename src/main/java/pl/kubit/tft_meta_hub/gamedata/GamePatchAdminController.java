package pl.kubit.tft_meta_hub.gamedata;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

/** The "Patches" section of the admin panel. Outcomes are shown as flash messages after a redirect (PRG). */
@Controller
@RequiredArgsConstructor
class GamePatchAdminController {

	static final String SUCCESS_MESSAGE = "successMessage";
	static final String ERROR_MESSAGE = "errorMessage";

	private final GamePatchService gamePatchService;

	@PostMapping("/admin/patches")
	String add(@Valid @ModelAttribute GamePatchForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			String errors = bindingResult.getFieldErrors().stream()
					.map(error -> error.getField() + " " + (error.isBindingFailure() ? "has an invalid value" : error.getDefaultMessage()))
					.sorted()
					.collect(Collectors.joining("; "));
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Patch not added: " + errors + ".");
			return "redirect:/admin";
		}
		try {
			gamePatchService.add(form);
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Patch " + form.getPatch().strip() + " added.");
		} catch (GamePatchException e) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
		}
		return "redirect:/admin";
	}

	@PostMapping("/admin/patches/{id}/delete")
	String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
		gamePatchService.delete(id);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Patch removed.");
		return "redirect:/admin";
	}
}
