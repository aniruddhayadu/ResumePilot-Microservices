package com.resumepilot.resume.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeConfigTest {

    @Test
    void securityConfigProvidesExpectedCorsConfiguration() {
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/resume/my-resumes"));

        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigRegistersMappingsWithoutError() {
        CorsRegistry registry = new CorsRegistry();

        new CorsConfig().addCorsMappings(registry);

        assertThat(registry).isNotNull();
    }
}
