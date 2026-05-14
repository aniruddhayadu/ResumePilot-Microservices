package com.resumepilot.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 60_000L);
    }

    @Test
    void generateTokenCanBeParsedAndValidated() {
        String token = jwtUtil.generateToken("palak@example.com", "FREE");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("palak@example.com");
        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("FREE");
        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtUtil.validateToken(token, "palak@example.com")).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForDifferentEmail() {
        String token = jwtUtil.generateToken("palak@example.com", "FREE");

        assertThat(jwtUtil.validateToken(token, "other@example.com")).isFalse();
    }
}
