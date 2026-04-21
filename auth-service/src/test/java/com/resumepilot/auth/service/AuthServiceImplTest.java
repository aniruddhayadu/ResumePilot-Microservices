package com.resumepilot.auth.service;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;
import com.resumepilot.auth.entity.User;
import com.resumepilot.auth.repository.UserRepository;
import com.resumepilot.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository repo;

	@Mock
	private PasswordEncoder enc;

	@Mock
	private JwtUtil jwt;

	@InjectMocks
	private AuthServiceImpl authService;

	private RegisterRequest registerRequest;
	private User dummyUser;

	@BeforeEach
	void setUp() {
		// Dummy data for testing
		registerRequest = new RegisterRequest();
		registerRequest.setFullName("Aditya Jayswal");
		registerRequest.setEmail("aditya@test.com");
		registerRequest.setPassword("Password@123");
		registerRequest.setPhone("9876543210");

		dummyUser = new User();
		dummyUser.setFullName("Aditya Jayswal");
		dummyUser.setEmail("aditya@test.com");
		dummyUser.setPasswordHash("hashed_password_123");
		dummyUser.setRole("USER");
	}

	@Test
	void testRegister_Success() {
		// Mocking database and utilities
		when(repo.existsByEmail(anyString())).thenReturn(false);
		when(enc.encode(anyString())).thenReturn("hashed_password_123");
		when(repo.save(any(User.class))).thenReturn(dummyUser);
		when(jwt.generateToken(anyString(), anyString())).thenReturn("mocked_jwt_token_xyz");

		// Action
		AuthResponse response = authService.register(registerRequest);

		// Verification
		assertNotNull(response);
		assertEquals("mocked_jwt_token_xyz", response.getToken());
		assertEquals("USER", response.getRole());
		assertEquals("Aditya Jayswal", response.getFullName());

		// Ensure repo.save was called exactly once
		verify(repo, times(1)).save(any(User.class));
	}

	@Test
	void testRegister_EmailAlreadyInUse() {
		// Mocking scenario where email is already in database
		when(repo.existsByEmail(anyString())).thenReturn(true);

		// Action & Verification
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			authService.register(registerRequest);
		});

		assertEquals("Email already in use", exception.getMessage());
		verify(repo, never()).save(any(User.class)); // Database save should not happen
	}

	@Test
	void testLogin_Success() {
		AuthRequest loginReq = new AuthRequest("aditya@test.com", "Password@123");

		when(repo.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
		when(enc.matches(anyString(), anyString())).thenReturn(true);
		when(jwt.generateToken(anyString(), anyString())).thenReturn("mocked_jwt_token_login");

		AuthResponse response = authService.login(loginReq);

		assertNotNull(response);
		assertEquals("mocked_jwt_token_login", response.getToken());
	}
}