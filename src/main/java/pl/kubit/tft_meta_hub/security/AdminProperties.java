package pl.kubit.tft_meta_hub.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The only user of the application. Values come from the {@code ADMIN_USERNAME} and {@code ADMIN_PASSWORD}
 * environment variables; the application does not start when they are missing.
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(@NotBlank String username, @NotBlank String password) {
}
