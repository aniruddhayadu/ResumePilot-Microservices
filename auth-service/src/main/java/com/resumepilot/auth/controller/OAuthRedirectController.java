package com.resumepilot.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class OAuthRedirectController {

	@Value("${app.frontend-url:http://localhost:5173}")
	private String frontendUrl = "http://localhost:5173";

	@GetMapping("/login")
	public void redirectLoginError(HttpServletResponse response) throws IOException {
		String message = URLEncoder.encode("Google login failed. Please try again.", StandardCharsets.UTF_8);
		response.sendRedirect(frontendUrl + "/?oauthError=" + message);
	}
}
