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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository repo;

    @Mock
    private PasswordEncoder enc;

    @Mock
    private JwtUtil jwt;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Palak", " PALAK@Example.COM ", "Password@123", "7894561235");

        user = new User();
        user.setUserId(1L);
        user.setFullName("Palak");
        user.setEmail("palak@example.com");
        user.setPasswordHash("hashed");
        user.setRole("FREE");
        user.setSubscriptionPlan("FREE");
        user.setVerified(true);
        user.setActive(true);
    }

    @Test
    void registerCreatesFreeUnverifiedUserAndSendsOtp() {
        when(repo.existsByEmail("palak@example.com")).thenReturn(false);
        when(enc.encode("Password@123")).thenReturn("hashed");
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isNull();
        assertThat(response.getRole()).isEqualTo("FREE");
        assertThat(response.getFullName()).isEqualTo("Palak");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repo).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("palak@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo("FREE");
        assertThat(saved.isVerified()).isFalse();
        assertThat(saved.getOtp()).hasSize(6);
        assertThat(saved.getOtpExpiry()).isAfter(LocalDateTime.now());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getTo()).containsExactly("palak@example.com");
        assertThat(mailCaptor.getValue().getText()).contains(saved.getOtp());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(repo.existsByEmail("palak@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already in use");

        verify(repo, never()).save(any(User.class));
    }

    @Test
    void loginReturnsJwtForVerifiedUser() {
        when(repo.findByEmail("palak@example.com")).thenReturn(Optional.of(user));
        when(enc.matches("Password@123", "hashed")).thenReturn(true);
        when(jwt.generateToken("palak@example.com", "FREE")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new AuthRequest(" PALAK@Example.COM ", "Password@123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("FREE");
        assertThat(response.getFullName()).isEqualTo("Palak");
    }

    @Test
    void loginRejectsUnverifiedUser() {
        user.setVerified(false);
        when(repo.findByEmail("palak@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthRequest("palak@example.com", "Password@123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Please verify your email first!");
    }

    @Test
    void verifyOtpActivatesUser() {
        user.setVerified(false);
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(repo.findByEmail("palak@example.com")).thenReturn(Optional.of(user));

        authService.verifyOtp(" PALAK@example.com ", "123456");

        assertThat(user.isVerified()).isTrue();
        assertThat(user.getOtp()).isNull();
        assertThat(user.getOtpExpiry()).isNull();
        verify(repo).save(user);
    }

    @Test
    void verifyOtpRejectsExpiredOtp() {
        user.setVerified(false);
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        when(repo.findByEmail("palak@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyOtp("palak@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP has expired. Please register again.");
    }

    @Test
    void oauthCreatesVerifiedFreeUserWhenMissing() {
        when(repo.findByEmail("google@example.com")).thenReturn(Optional.empty());
        when(jwt.generateToken("google@example.com", "FREE")).thenReturn("oauth-token");
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.processOAuthPostLogin(" Google@Example.COM ", "Google User");

        assertThat(response.getToken()).isEqualTo("oauth-token");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repo).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isVerified()).isTrue();
    }

    @Test
    void forgotPasswordStoresResetTokenAndSendsEmail() {
        when(repo.findByEmail("palak@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("palak@example.com");

        assertThat(user.getResetToken()).isNotBlank();
        assertThat(user.getResetTokenExpiry()).isAfter(LocalDateTime.now());
        verify(repo).save(user);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPasswordUpdatesHashAndClearsToken() {
        user.setResetToken("token");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));
        when(repo.findByResetToken("token")).thenReturn(Optional.of(user));
        when(enc.encode("NewPassword@123")).thenReturn("new-hash");

        authService.resetPassword("token", "NewPassword@123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getResetToken()).isNull();
        assertThat(user.getResetTokenExpiry()).isNull();
        verify(repo).save(user);
    }
}
