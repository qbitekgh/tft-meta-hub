package pl.kubit.tft_meta_hub.gamedata.cdragon;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Access to {@code cdragon/tft-sample.json}: a small excerpt of the real Community Dragon file (set 18)
 * with 5 champions (incl. a non-playable unit), 6 traits (incl. one with null minUnits) and 13 items of various kinds.
 */
public final class CdragonFixtures {

	public static final String SAMPLE_PATH = "/cdragon/tft-sample.json";

	public static final CommunityDragonProperties PROPERTIES = new CommunityDragonProperties(
			"https://cdragon.test/latest", "/cdragon/tft/en_us.json", "https://cdragon.test/latest/game/");

	private CdragonFixtures() {
	}

	public static String sampleJson() {
		try (InputStream input = CdragonFixtures.class.getResourceAsStream(SAMPLE_PATH)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static CdragonData sampleData() {
		return JsonMapper.builder().build().readValue(sampleJson(), CdragonData.class);
	}
}
