package com.resumepilot.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final CustomOAuth2SuccessHandler oauth2SuccessHandler;

	@Value("${app.frontend-url:http://localhost:5173}")
	private String frontendUrl = "http://localhost:5173";

	@Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
	private String corsAllowedOrigins;

	public SecurityConfig(JwtFilter jwtFilter, CustomOAuth2SuccessHandler oauth2SuccessHandler) {
		this.jwtFilter = jwtFilter;
		this.oauth2SuccessHandler = oauth2SuccessHandler;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(parseOrigins(corsAllowedOrigins));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			config.setAllowedHeaders(List.of("*"));
			config.setAllowCredentials(true);
			return config;
		})).csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// 🚀 Sabse important change: /api/admin/** ko rasta de diya
						.requestMatchers("/auth/**", "/oauth2/**", "/login", "/login/**", "/api/admin/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(frontendAwareEntryPoint()))
				.oauth2Login(oauth2 -> oauth2
						.loginPage("/oauth2/authorization/google")
						.successHandler(oauth2SuccessHandler)
						.failureHandler((request, response, exception) -> {
							String message = URLEncoder.encode("Google login failed. Please try again.",
									StandardCharsets.UTF_8);
							response.sendRedirect(frontendUrl + "/?oauthError=" + message);
						}))
				.formLogin(form -> form.disable())
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private AuthenticationEntryPoint frontendAwareEntryPoint() {
		return (request, response, authException) -> {
			String path = request.getRequestURI();
			if (path != null && path.startsWith("/login")) {
				String message = URLEncoder.encode("Google login failed. Please try again.", StandardCharsets.UTF_8);
				response.sendRedirect(frontendUrl + "/?oauthError=" + message);
				return;
			}
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
		};
	}

	private List<String> parseOrigins(String origins) {
		return List.of(origins.split(",")).stream()
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
	}
}
