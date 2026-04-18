package com.resumepilot.auth.service;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest; 
import com.resumepilot.auth.entity.User;
import com.resumepilot.auth.repository.UserRepository;
import com.resumepilot.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService {

	private final UserRepository repo;
	private final PasswordEncoder enc;
	private final JwtUtil jwt;

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

		// 4. Set default system values as per Case Study
		u.setRole("USER");
		u.setSubscriptionPlan("FREE");
		u.setActive(true);

		repo.save(u);

		// 5. Generate and return JWT [cite: 183, 184]
		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public AuthResponse login(AuthRequest req) {
		// 1. Fetch user by email [cite: 211, 244]
		User u = repo.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid email or password"));

		// 2. Verify hashed password [cite: 194, 244]
		if (!enc.matches(req.getPassword(), u.getPasswordHash())) {
			throw new RuntimeException("Invalid email or password");
		}

		// 3. Generate JWT [cite: 183, 244]
		String token = jwt.generateToken(u.getEmail(), u.getRole());
		return new AuthResponse(token, u.getRole(), u.getFullName());
	}

	@Override
	public AuthResponse register(User u) {
		// TODO Auto-generated method stub
		return null;
	}
}