package pl.kubit.tft_meta_hub.explorer;

import java.util.List;

/**
 * The explorer's URL parameters exactly as they arrive. Values are not trusted: {@link ExplorerState#from} replaces
 * anything missing or invalid with a default, so an edited or outdated link still shows a page.
 *
 * @param f filters, one {@link ExplorerFilter#toParam()} value each
 */
public record ExplorerRequest(String patch,
							  String server,
							  Integer days,
							  List<String> f,
							  String tab,
							  String holder,
							  Boolean split,
							  Integer cost,
							  String sort,
							  Integer minGames) {

	public static ExplorerRequest empty() {
		return new ExplorerRequest(null, null, null, null, null, null, null, null, null, null);
	}
}
