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
import java.util.Optional;

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
        u.setRole("USER");
        u.setSubscriptionPlan("FREE");
        u.setActive(true);
        repo.save(u);

        String token = jwt.generateToken(u.getEmail(), u.getRole());
        return new AuthResponse(token, u.getRole(), u.getFullName());
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        User u = repo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!enc.matches(req.getPassword(), u.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwt.generateToken(u.getEmail(), u.getRole());
        return new AuthResponse(token, u.getRole(), u.getFullName());
    }

    // New Method for Google OAuth2
    @Override
    public AuthResponse processOAuthPostLogin(String email, String name) {
        Optional<User> userOptional = repo.findByEmail(email);
        
        User u;
        if (userOptional.isEmpty()) {
            // First time Google Login: Create new user
            u = new User();
            u.setEmail(email);
            u.setFullName(name);
            u.setPasswordHash("OAUTH2_USER"); // Placeholder
            u.setRole("USER");
            u.setSubscriptionPlan("FREE");
            u.setActive(true);
            repo.save(u);
        } else {
            u = userOptional.get();
        }

        String token = jwt.generateToken(u.getEmail(), u.getRole());
        return new AuthResponse(token, u.getRole(), u.getFullName());
    }

    @Override
    public AuthResponse register(User u) {
        return null; // Keep it dummy or remove from interface if not needed
    }
}