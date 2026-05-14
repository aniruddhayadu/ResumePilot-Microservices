package com.resumepilot.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipPublicAuthOAuthAndLoginPaths() {
        TestableJwtFilter filter = new TestableJwtFilter(jwtUtil);

        assertThat(filter.shouldSkip(request("/auth/login"))).isTrue();
        assertThat(filter.shouldSkip(request("/oauth2/callback"))).isTrue();
        assertThat(filter.shouldSkip(request("/login/oauth2/code/google"))).isTrue();
        assertThat(filter.shouldSkip(request("/api/profile"))).isFalse();
    }

    @Test
    void validBearerTokenPopulatesSecurityContext() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = request("/api/profile");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.extractEmail("jwt-token")).thenReturn("palak@example.com");
        when(jwtUtil.validateToken("jwt-token", "palak@example.com")).thenReturn(true);
        when(jwtUtil.extractRole("jwt-token")).thenReturn("ADMIN");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("palak@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeaderLeavesSecurityContextEmpty() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = request("/api/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).extractEmail("jwt-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidBearerTokenDoesNotAuthenticate() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = request("/api/profile");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.extractEmail("jwt-token")).thenReturn("palak@example.com");
        when(jwtUtil.validateToken("jwt-token", "palak@example.com")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredBearerTokenDoesNotBreakRequest() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = request("/api/profile");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.extractEmail("expired-token")).thenThrow(new IllegalArgumentException("expired"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void existingAuthenticationSkipsTokenValidation() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = request("/api/profile");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing@example.com", null));
        when(jwtUtil.extractEmail("jwt-token")).thenReturn("palak@example.com");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("existing@example.com");
        verify(jwtUtil, never()).validateToken("jwt-token", "palak@example.com");
    }

    private MockHttpServletRequest request(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        request.setRequestURI(servletPath);
        return request;
    }

    private static class TestableJwtFilter extends JwtFilter {

        TestableJwtFilter(JwtUtil jwtUtil) {
            super(jwtUtil);
        }

        boolean shouldSkip(HttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
