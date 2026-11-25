package pl.kubit.tft_meta_hub.riot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Riot platforms (servers) the application collects from. League endpoints are called on the platform host
 * ({@code eun1.api.riotgames.com}); match endpoints of both platforms share the regional host {@code europe}.
 */
@Getter
@RequiredArgsConstructor
public enum Platform {
	EUNE("eun1"),
	EUW("euw1");

	/** Host prefix of the platform routing value, e.g. "eun1". */
	private final String host;

	/** Match ids start with the platform, e.g. "EUN1_4027724406" or "EUW1_8049675988". */
	public static Optional<Platform> fromMatchId(String matchId) {
		return Arrays.stream(values())
				.filter(platform -> matchId.toLowerCase(Locale.ROOT).startsWith(platform.host + "_"))
				.findFirst();
	}
}
