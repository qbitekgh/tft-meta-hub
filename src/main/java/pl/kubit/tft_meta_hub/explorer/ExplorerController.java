package pl.kubit.tft_meta_hub.explorer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Public explorer page. Everything is a GET request: the URL holds the whole state of the page.
 */
@Controller
@RequiredArgsConstructor
class ExplorerController {

	private static final ExplorerLinks LINKS = new ExplorerLinks();

	private final ExplorerService explorerService;

	@GetMapping(ExplorerLinks.PATH)
	String explorer(@ModelAttribute ExplorerRequest request, Model model) {
		model.addAttribute("page", explorerService.explore(request));
		model.addAttribute("links", LINKS);
		return "explorer";
	}

	/**
	 * Target of the "add filter" forms: turns the form fields into one {@code f} parameter and redirects to the
	 * explorer, so the address bar shows the same short link that the page itself would build.
	 */
	@GetMapping(ExplorerLinks.PATH + "/add-filter")
	String addFilter(@ModelAttribute ExplorerRequest request, @ModelAttribute NewFilterRequest newFilter) {
		ExplorerState state = explorerService.resolveState(request);
		ExplorerState withFilter = newFilter.toFilter()
				.map(filter -> state.withFiltersAdded(List.of(filter)))
				.orElse(state);
		return "redirect:" + LINKS.url(withFilter);
	}
}
