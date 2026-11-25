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

/**
 * Admin pages for tier lists: create, edit details, copy to a new patch, publish, delete.
 * Thin by design: validation errors and business rule messages are shown, everything else is delegated
 * to {@link TierListService}.
 */
@Controller
@RequestMapping("/admin/tierlists")
@RequiredArgsConstructor
class TierListAdminController {

	static final String FORM_VIEW = "admin/tierlists/form";
	static final String SUCCESS_MESSAGE = "successMessage";
	static final String ERROR_MESSAGE = "errorMessage";

	private final TierListService tierListService;

	@GetMapping("/new")
	String newForm(Model model) {
		model.addAttribute("tierListForm", new TierListForm());
		return showForm(model, FormMode.CREATE, null);
	}

	@PostMapping
	String create(@Valid @ModelAttribute TierListForm tierListForm, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return showForm(model, FormMode.CREATE, null);
		}
		try {
			long id = tierListService.createDraft(tierListForm);
			return redirectToDetails(id);
		} catch (TierListRuleException e) {
			bindingResult.reject("tierList.rule", e.getMessage());
			return showForm(model, FormMode.CREATE, null);
		}
	}

	@GetMapping("/{id}")
	String details(@PathVariable long id, Model model) {
		model.addAttribute("tierList", tierListService.getDetails(id));
		return "admin/tierlists/details";
	}

	@GetMapping("/{id}/edit")
	String editForm(@PathVariable long id, Model model) {
		TierListDetails tierList = tierListService.getDetails(id);
		model.addAttribute("tierListForm", new TierListForm(tierList.title(), tierList.patch(), tierList.setNumber()));
		return showForm(model, FormMode.EDIT, id);
	}

	@PostMapping("/{id}")
	String update(@PathVariable long id,
				  @Valid @ModelAttribute TierListForm tierListForm,
				  BindingResult bindingResult,
				  Model model,
				  RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return showForm(model, FormMode.EDIT, id);
		}
		try {
			tierListService.updateDetails(id, tierListForm);
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tier list saved.");
			return redirectToDetails(id);
		} catch (TierListRuleException e) {
			bindingResult.reject("tierList.rule", e.getMessage());
			return showForm(model, FormMode.EDIT, id);
		}
	}

	@GetMapping("/{id}/copy")
	String copyForm(@PathVariable long id, Model model) {
		TierListDetails source = tierListService.getDetails(id);
		model.addAttribute("tierListForm", new TierListForm(source.title(), "", source.setNumber()));
		return showForm(model, FormMode.COPY, id);
	}

	@PostMapping("/{id}/copy")
	String copy(@PathVariable long id,
				@Valid @ModelAttribute TierListForm tierListForm,
				BindingResult bindingResult,
				Model model,
				RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return showForm(model, FormMode.COPY, id);
		}
		long copyId = tierListService.copyToNewPatch(id, tierListForm);
		redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Copy created as a new draft.");
		return redirectToDetails(copyId);
	}

	@PostMapping("/{id}/publish")
	String publish(@PathVariable long id, RedirectAttributes redirectAttributes) {
		try {
			tierListService.publish(id);
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tier list published.");
		} catch (TierListRuleException e) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
		}
		return redirectToDetails(id);
	}

	@PostMapping("/{id}/delete")
	String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
		try {
			tierListService.delete(id);
			redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tier list deleted.");
			return "redirect:/admin";
		} catch (TierListRuleException e) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
			return redirectToDetails(id);
		}
	}

	private static String showForm(Model model, FormMode mode, Long tierListId) {
		model.addAttribute("mode", mode);
		model.addAttribute("tierListId", tierListId);
		return FORM_VIEW;
	}

	private static String redirectToDetails(long id) {
		return "redirect:/admin/tierlists/" + id;
	}

	/** The same form template serves three use cases. */
	enum FormMode {
		CREATE, EDIT, COPY
	}
}
