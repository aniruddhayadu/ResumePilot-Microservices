package com.resumepilot.template.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeExceptionReturnsInternalServerError() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "boom");
        assertThat(response.getBody()).containsEntry("status", "500");
    }

    @Test
    void handleNotFoundReturnsBadRequest() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(
                new IllegalArgumentException("Template not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Template not found");
    }
}
