package com.resumepilot.auth.controller;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;
import com.resumepilot.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthResource {

	private final AuthService svc;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
		return ResponseEntity.ok(svc.register(req));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
		return ResponseEntity.ok(svc.login(req));
	}

	@GetMapping("/oauth2-success")
	public ResponseEntity<AuthResponse> googleLoginSuccess(@AuthenticationPrincipal OAuth2User principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		String email = principal.getAttribute("email");
		String name = principal.getAttribute("name");
		return ResponseEntity.ok(svc.processOAuthPostLogin(email, name));
	}

	// NAYE ENDPOINTS FORGOT PASSWORD KE LIYE
	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
		String email = payload.get("email");
		svc.forgotPassword(email);
		return ResponseEntity.ok("Password reset link has been sent to your email.");
	}

	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> payload) {
		String token = payload.get("token");
		String newPassword = payload.get("newPassword");
		svc.resetPassword(token, newPassword);
		return ResponseEntity.ok("Password has been reset successfully.");
	}
}