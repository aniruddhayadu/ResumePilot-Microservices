package com.resumepilot.auth.service;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;

public interface AuthService {
	AuthResponse register(RegisterRequest req);

	AuthResponse login(AuthRequest req);

	AuthResponse processOAuthPostLogin(String email, String name);

	void forgotPassword(String email);

	void resetPassword(String token, String newPassword);

	void verifyOtp(String email, String otp);
}