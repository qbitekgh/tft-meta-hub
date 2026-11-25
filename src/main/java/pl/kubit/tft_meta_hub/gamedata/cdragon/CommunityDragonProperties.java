package pl.kubit.tft_meta_hub.gamedata.cdragon;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param baseUrl       e.g. {@code https://raw.communitydragon.org/latest}
 * @param dataPath      path of the TFT data file, relative to {@code baseUrl}
 * @param assetsBaseUrl prefix for converted icon paths, ending with a slash
 */
@Validated
@ConfigurationProperties(prefix = "app.cdragon")
public record CommunityDragonProperties(@NotBlank String baseUrl,
										@NotBlank String dataPath,
										@NotBlank String assetsBaseUrl) {
}
