package pl.kubit.tft_meta_hub.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Public site with one protected area: everything under {@code /admin} requires the ADMIN role.
 * Login uses a form; CSRF protection stays enabled (Thymeleaf adds the token to every POST form).
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class SecurityConfig {

	static final String ADMIN_ROLE = "ADMIN";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/admin/**").hasRole(ADMIN_ROLE)
						.anyRequest().permitAll())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/admin")
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/"));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** The admin's plain-text password from configuration is hashed with BCrypt once, at startup. */
	@Bean
	UserDetailsService userDetailsService(AdminProperties admin, PasswordEncoder passwordEncoder) {
		UserDetails adminUser = User.withUsername(admin.username())
				.password(passwordEncoder.encode(admin.password()))
				.roles(ADMIN_ROLE)
				.build();
		return new InMemoryUserDetailsManager(adminUser);
	}
}
