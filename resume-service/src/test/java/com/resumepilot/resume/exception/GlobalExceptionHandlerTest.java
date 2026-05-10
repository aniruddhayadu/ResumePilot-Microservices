package com.resumepilot.resume.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundReturnsNotFoundBody() {
        ResponseEntity<Map<String, String>> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Resume not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Resume not found");
    }

    @Test
    void handleGeneralExceptionReturnsInternalServerErrorBody() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneralException(new Exception("DB down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Internal error: DB down");
    }
}
