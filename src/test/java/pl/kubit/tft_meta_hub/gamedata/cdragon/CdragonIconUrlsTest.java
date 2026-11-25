package pl.kubit.tft_meta_hub.gamedata.cdragon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CdragonIconUrlsTest {

	private final CdragonIconUrls iconUrls = new CdragonIconUrls(CdragonFixtures.PROPERTIES);

	@Test
	void shouldReplaceTexExtensionWithPng_whenPathIsTexture() {
		String url = iconUrls.toUrl("assets/ux/traiticons/trait_icon_17_rogue.tex");

		assertThat(url).isEqualTo("https://cdragon.test/latest/game/assets/ux/traiticons/trait_icon_17_rogue.png");
	}

	@Test
	void shouldLowerCasePath_whenPathContainsUpperCaseLetters() {
		String url = iconUrls.toUrl("ASSETS/Characters/TFT18_Lux/tft18_lux_square.TEX");

		assertThat(url).isEqualTo("https://cdragon.test/latest/game/assets/characters/tft18_lux/tft18_lux_square.png");
	}

	@Test
	void shouldKeepExtension_whenPathIsAlreadyPng() {
		String url = iconUrls.toUrl("assets/ux/icon.png");

		assertThat(url).isEqualTo("https://cdragon.test/latest/game/assets/ux/icon.png");
	}

	@Test
	void shouldReturnNull_whenPathIsMissingOrHash() {
		assertThat(iconUrls.toUrl(null)).isNull();
		assertThat(iconUrls.toUrl(" ")).isNull();
		assertThat(iconUrls.toUrl("{f3b734c3102f71ed}")).isNull();
	}
}
