package com.resumepilot.auth.security;

import com.resumepilot.auth.dto.AuthResponse;
import com.resumepilot.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomOAuth2SuccessHandlerTest {

    @Test
    void onAuthenticationSuccessRedirectsToFrontendWithTokenAndName() throws Exception {
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("google@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Google User");
        when(authService.processOAuthPostLogin("google@example.com", "Google User"))
                .thenReturn(new AuthResponse("oauth-token", "USER", "Google User"));

        new CustomOAuth2SuccessHandler(authService).onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/?token=oauth-token&userName=Google+User");
    }
}
