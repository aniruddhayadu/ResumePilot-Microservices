package com.resumepilot.auth.controller;

import com.resumepilot.auth.dto.AuthRequest;
import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.dto.RegisterRequest;
import com.resumepilot.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private OAuth2User principal;

    @Test
    void registerDelegatesToAuthService() {
        AuthResource controller = new AuthResource(authService);
        RegisterRequest request = new RegisterRequest("Palak", "palak@example.com", "Password@123", "7894561235");
        AuthResponse serviceResponse = new AuthResponse(null, "FREE", "Palak");
        when(authService.register(request)).thenReturn(serviceResponse);

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(authService).register(request);
    }

    @Test
    void loginDelegatesToAuthService() {
        AuthResource controller = new AuthResource(authService);
        AuthRequest request = new AuthRequest("palak@example.com", "Password@123");
        AuthResponse serviceResponse = new AuthResponse("jwt", "FREE", "Palak");
        when(authService.login(request)).thenReturn(serviceResponse);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(authService).login(request);
    }

    @Test
    void verifyOtpDelegatesToAuthService() {
        AuthResource controller = new AuthResource(authService);

        ResponseEntity<String> response = controller.verifyOtp(Map.of("email", "palak@example.com", "otp", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Email verified successfully.");
        verify(authService).verifyOtp("palak@example.com", "123456");
    }

    @Test
    void googleLoginSuccessReturnsUnauthorizedWhenPrincipalMissing() {
        AuthResource controller = new AuthResource(authService);

        ResponseEntity<AuthResponse> response = controller.googleLoginSuccess(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void googleLoginSuccessDelegatesWhenPrincipalExists() {
        AuthResource controller = new AuthResource(authService);
        AuthResponse serviceResponse = new AuthResponse("oauth-jwt", "FREE", "Google User");
        when(principal.getAttribute("email")).thenReturn("google@example.com");
        when(principal.getAttribute("name")).thenReturn("Google User");
        when(authService.processOAuthPostLogin("google@example.com", "Google User")).thenReturn(serviceResponse);

        ResponseEntity<AuthResponse> response = controller.googleLoginSuccess(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
    }

    @Test
    void forgotPasswordDelegatesToAuthService() {
        AuthResource controller = new AuthResource(authService);
        when(authService.forgotPassword("palak@example.com"))
                .thenReturn("Password reset link has been sent to your email.");

        ResponseEntity<String> response = controller.forgotPassword(Map.of("email", "palak@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Password reset link has been sent to your email.");
        verify(authService).forgotPassword("palak@example.com");
    }

    @Test
    void resetPasswordDelegatesToAuthService() {
        AuthResource controller = new AuthResource(authService);

        ResponseEntity<String> response = controller.resetPassword(
                Map.of("token", "reset-token", "newPassword", "NewPassword@123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Password has been reset successfully.");
        verify(authService).resetPassword("reset-token", "NewPassword@123");
    }
}
