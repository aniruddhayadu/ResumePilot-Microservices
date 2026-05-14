package com.resumepilot.auth.security;

import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	@Value("${app.frontend-url:http://localhost:5173}")
	private String frontendUrl = "http://localhost:5173";

	public CustomOAuth2SuccessHandler(@Lazy AuthService authService) {
		this.authService = authService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");
		String name = oAuth2User.getAttribute("name");

		AuthResponse authResponse = authService.processOAuthPostLogin(email, name);
		String token = authResponse.getToken();
		String displayName = authResponse.getFullName() != null ? authResponse.getFullName() : name;

		String targetUrl = frontendUrl + "/?token=" + urlEncode(token) + "&userName=" + urlEncode(displayName);

		response.sendRedirect(targetUrl);
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}
}
