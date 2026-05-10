package com.resumepilot.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationControllerTest {

    @Test
    void checkHealthReturnsUpMessage() {
        ResponseEntity<String> response = new NotificationController().checkHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Notification Service is UP");
    }
}
