package pl.kubit.tft_meta_hub.tierlist;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.kubit.tft_meta_hub.tierlist.TierListDetails.CompositionView;

/** Admin pages for adding, editing and removing compositions of a tier list. */
@Controller
@RequestMapping("/admin/tierlists/{tierListId}/compositions")
@RequiredArgsConstructor
class CompositionAdminController {

	static final String FORM_VIEW = "admin/tierlists/composition-form";

	private final TierListService tierListService;

	@GetMapping("/new")
	String newForm(@PathVariable long tierListId, Model model) {
		model.addAttribute("compositionForm", CompositionForm.empty());
		return showForm(model, tierListId, null);
	}

	@PostMapping
	String create(@PathVariable long tierListId,
				  @Valid @ModelAttribute CompositionForm compositionForm,
				  BindingResult bindingResult,
				  Model model,
				  RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return showForm(model, tierListId, null);
		}
		try {
			tierListService.addComposition(tierListId, compositionForm);
			redirectAttributes.addFlashAttribute(TierListAdminController.SUCCESS_MESSAGE, "Composition added.");
			return redirectToTierList(tierListId);
		} catch (TierListRuleException e) {
			bindingResult.reject("composition.rule", e.getMessage());
			return showForm(model, tierListId, null);
		}
	}

	@GetMapping("/{compositionId}/edit")
	String editForm(@PathVariable long tierListId, @PathVariable long compositionId, Model model) {
		CompositionView composition = tierListService.getDetails(tierListId).compositions().stream()
				.filter(view -> view.id() == compositionId)
				.findFirst()
				.orElseThrow(() -> new TierListNotFoundException("Composition " + compositionId + " not found"));
		model.addAttribute("compositionForm", CompositionForm.from(composition));
		return showForm(model, tierListId, compositionId);
	}

	@PostMapping("/{compositionId}")
	String update(@PathVariable long tierListId,
				  @PathVariable long compositionId,
				  @Valid @ModelAttribute CompositionForm compositionForm,
				  BindingResult bindingResult,
				  Model model,
				  RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return showForm(model, tierListId, compositionId);
		}
		try {
			tierListService.updateComposition(tierListId, compositionId, compositionForm);
			redirectAttributes.addFlashAttribute(TierListAdminController.SUCCESS_MESSAGE, "Composition saved.");
			return redirectToTierList(tierListId);
		} catch (TierListRuleException e) {
			bindingResult.reject("composition.rule", e.getMessage());
			return showForm(model, tierListId, compositionId);
		}
	}

	@PostMapping("/{compositionId}/delete")
	String delete(@PathVariable long tierListId, @PathVariable long compositionId, RedirectAttributes redirectAttributes) {
		try {
			tierListService.deleteComposition(tierListId, compositionId);
			redirectAttributes.addFlashAttribute(TierListAdminController.SUCCESS_MESSAGE, "Composition removed.");
		} catch (TierListRuleException e) {
			redirectAttributes.addFlashAttribute(TierListAdminController.ERROR_MESSAGE, e.getMessage());
		}
		return redirectToTierList(tierListId);
	}

	private String showForm(Model model, long tierListId, Long compositionId) {
		CompositionForm form = (CompositionForm) model.getAttribute("compositionForm");
		if (form != null) {
			form.fillEmptyRows();
		}
		model.addAttribute("options", tierListService.getCompositionFormOptions(tierListId));
		model.addAttribute("compositionId", compositionId);
		model.addAttribute("tiers", Tier.values());
		model.addAttribute("playStyles", PlayStyle.values());
		model.addAttribute("difficulties", Difficulty.values());
		return FORM_VIEW;
	}

	private static String redirectToTierList(long tierListId) {
		return "redirect:/admin/tierlists/" + tierListId;
	}
}
