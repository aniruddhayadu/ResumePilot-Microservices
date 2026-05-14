package com.resumepilot.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateConfigTest {

    @Test
    void openApiConfigDefinesBearerAuth() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Template Service API");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }

    @Test
    void securityConfigProvidesCorsConfiguration() {
        UrlBasedCorsConfigurationSource source = new SecurityConfig(new JwtAuthenticationFilter())
                .corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("POST", "/api/payments/verify"));

        assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isTrue();
    }
}
