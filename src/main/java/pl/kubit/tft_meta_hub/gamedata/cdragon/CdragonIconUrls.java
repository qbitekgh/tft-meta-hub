package pl.kubit.tft_meta_hub.gamedata.cdragon;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Converts game asset paths from the data file into image URLs, e.g.
 * {@code assets/ux/traiticons/trait_icon_17_rogue.tex} becomes
 * {@code https://raw.communitydragon.org/latest/game/assets/ux/traiticons/trait_icon_17_rogue.png}.
 */
@Component
public class CdragonIconUrls {

	private static final String TEXTURE_EXTENSION = ".tex";
	private static final String IMAGE_EXTENSION = ".png";

	private final String assetsBaseUrl;

	public CdragonIconUrls(CommunityDragonProperties properties) {
		this.assetsBaseUrl = properties.assetsBaseUrl();
	}

	/** Returns {@code null} when the path is missing or is an unresolved hash such as {@code {f3b734c3102f71ed}}. */
	public String toUrl(String assetPath) {
		if (assetPath == null || assetPath.isBlank() || assetPath.startsWith("{")) {
			return null;
		}
		String path = assetPath.toLowerCase(Locale.ROOT);
		if (path.endsWith(TEXTURE_EXTENSION)) {
			path = path.substring(0, path.length() - TEXTURE_EXTENSION.length()) + IMAGE_EXTENSION;
		}
		return assetsBaseUrl + path;
	}
}
