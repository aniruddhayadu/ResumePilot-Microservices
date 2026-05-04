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
	private final JavaMailSender mailSender;

	@Override
	public AuthResponse register(RegisterRequest req) {
		String cleanEmail = req.getEmail().toLowerCase().trim();

		if (repo.existsByEmail(cleanEmail)) {
			throw new RuntimeException("Email already in use");
		}

		String generatedOtp = String.valueOf((int) (Math.random() * 900000) + 100000);

		User u = new User();
		u.setFullName(req.getFullName());
		u.setEmail(cleanEmail);
		u.setPhone(req.getPhone());
		u.setPasswordHash(enc.encode(req.getPassword()));
		u.setRole("FREE");
		u.setSubscriptionPlan("FREE");
		u.setActive(true);
		u.setVerified(false);
		u.setOtp(generatedOtp);
		u.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

		repo.save(u);

		try {
			sendEmail(u.getEmail(), "ResumePilot - Verify Your Email",
					"Your OTP for registration is: " + generatedOtp + "\n\nThis OTP is valid for 10 minutes.");
		} catch (Exception e) {
			System.out.println("Error sending email: " + e.getMessage());
		}

		return new AuthResponse(null, "FREE", u.getFullName());
	}

	@Override
	public AuthResponse login(AuthRequest req) {
		String cleanEmail = req.getEmail().toLowerCase().trim();
		User u = repo.findByEmail(cleanEmail).orElseThrow(() -> new RuntimeException("Invalid email or password"));

		if (!u.isVerified()) {
			throw new RuntimeException("Please verify your email first!");
		}

		if (!enc.matches(req.getPassword(), u.getPasswordHash())) {
			throw new RuntimeException("Invalid email or password");
		}

		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public void verifyOtp(String email, String otp) {
		String cleanEmail = email.toLowerCase().trim();
		User u = repo.findByEmail(cleanEmail).orElseThrow(() -> new RuntimeException("User not found"));

		if (u.isVerified()) {
			throw new RuntimeException("User is already verified");
		}
		if (u.getOtpExpiry() == null || u.getOtpExpiry().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("OTP has expired. Please register again.");
		}
		if (!u.getOtp().equals(otp.trim())) {
			throw new RuntimeException("Invalid OTP entered.");
		}

		u.setVerified(true);
		u.setOtp(null);
		u.setOtpExpiry(null);
		repo.save(u);
	}

	@Override
	public AuthResponse processOAuthPostLogin(String email, String name) {
		String cleanEmail = email.toLowerCase().trim();
		Optional<User> userOptional = repo.findByEmail(cleanEmail);

		User u;
		if (userOptional.isEmpty()) {
			u = new User();
			u.setEmail(cleanEmail);
			u.setFullName(name);
			u.setPasswordHash("OAUTH2_USER");
			u.setRole("FREE");
			u.setSubscriptionPlan("FREE");
			u.setActive(true);
			u.setVerified(true);
			repo.save(u);
		} else {
			u = userOptional.get();
		}

		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public void forgotPassword(String email) {
		String cleanEmail = email.toLowerCase().trim();
		User user = repo.findByEmail(cleanEmail)
				.orElseThrow(() -> new RuntimeException("User not found with this email"));

		String token = UUID.randomUUID().toString();
		user.setResetToken(token);
		user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
		repo.save(user);

		String resetUrl = "http://localhost:5173/reset-password?token=" + token;
		sendEmail(user.getEmail(), "ResumePilot - Password Reset Request",
				"To reset your password, click the link below:\n\n" + resetUrl
						+ "\n\nThis link will expire in 15 minutes.");
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

	private void sendEmail(String to, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		mailSender.send(message);
	}
}