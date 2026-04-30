package com.resumepilot.auth.service;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;
import com.resumepilot.auth.entity.User;
import com.resumepilot.auth.repository.UserRepository;
import com.resumepilot.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository repo;
	private final PasswordEncoder enc;
	private final JwtUtil jwt;
	private final JavaMailSender mailSender; // NAYA: Email Bhejne ke liye

	@Override
	public AuthResponse register(RegisterRequest req) {
		if (repo.existsByEmail(req.getEmail())) {
			throw new RuntimeException("Email already in use");
		}

		User u = new User();
		u.setFullName(req.getFullName());
		u.setEmail(req.getEmail());
		u.setPhone(req.getPhone());
		u.setPasswordHash(enc.encode(req.getPassword()));
		u.setRole("FREE"); // NAYA: Default role Free
		u.setSubscriptionPlan("FREE");
		u.setActive(true);
		repo.save(u);

		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public AuthResponse login(AuthRequest req) {
		User u = repo.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid email or password"));

		if (!enc.matches(req.getPassword(), u.getPasswordHash())) {
			throw new RuntimeException("Invalid email or password");
		}

		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public AuthResponse processOAuthPostLogin(String email, String name) {
		Optional<User> userOptional = repo.findByEmail(email);

		User u;
		if (userOptional.isEmpty()) {
			u = new User();
			u.setEmail(email);
			u.setFullName(name);
			u.setPasswordHash("OAUTH2_USER");
			u.setRole("FREE"); // NAYA: Default role
			u.setSubscriptionPlan("FREE");
			u.setActive(true);
			repo.save(u);
		} else {
			u = userOptional.get();
		}

		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	// ================= FORGOT PASSWORD LOGIC =================

	@Override
	public void forgotPassword(String email) {
		User user = repo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found with this email"));

		// 1. Generate Token
		String token = UUID.randomUUID().toString();
		user.setResetToken(token);
		user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
		repo.save(user);

		// 2. Send Email
		String resetUrl = "http://localhost:5173/reset-password?token=" + token;
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(user.getEmail());
		message.setSubject("ResumePilot - Password Reset Request");
		message.setText("To reset your password, click the link below:\n\n" + resetUrl
				+ "\n\nThis link will expire in 15 minutes.");

		mailSender.send(message);
	}

	@Override
	public void resetPassword(String token, String newPassword) {
		User user = repo.findByResetToken(token)
				.orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

		if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("Token has expired. Please request a new one.");
		}

		user.setPasswordHash(enc.encode(newPassword));
		user.setResetToken(null);
		user.setResetTokenExpiry(null);
		repo.save(user);
	}
}