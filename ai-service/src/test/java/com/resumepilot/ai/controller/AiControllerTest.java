package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import com.resumepilot.ai.service.AiUsageLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @Test
    void generateSummaryConsumesQuotaAndReturnsHeaders() {
        AiUsageLimiter limiter = new AiUsageLimiter(5);
        AiController controller = new AiController(aiService, limiter);
        when(aiService.generateSummary("Java Developer")).thenReturn("Strong Java summary");

        ResponseEntity<Map<String, String>> response = controller.generateSummary(
                Map.of("jobTitle", "Java Developer"),
                "free@example.com",
                "FREE",
                "FREE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("generatedSummary", "Strong Java summary");
        assertThat(response.getHeaders().getFirst("X-AI-Free-Limit")).isEqualTo("5");
        assertThat(response.getHeaders().getFirst("X-AI-Remaining")).isEqualTo("4");
        verify(aiService).generateSummary("Java Developer");
    }

    @Test
    void freeUserGetsTooManyRequestsAfterLimit() {
        AiUsageLimiter limiter = new AiUsageLimiter(1);
        AiController controller = new AiController(aiService, limiter);
        when(aiService.analyzeResume("Java", "resume text")).thenReturn("{\"score\":80,\"feedback\":\"Good\"}");

        ResponseEntity<String> first = controller.analyzeResume(
                Map.of("jobTitle", "Java", "resumeContent", "resume text"),
                "free@example.com",
                "FREE",
                "FREE");
        ResponseEntity<String> second = controller.analyzeResume(
                Map.of("jobTitle", "Java", "resumeContent", "resume text"),
                "free@example.com",
                "FREE",
                "FREE");

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getHeaders().getFirst("X-AI-Remaining")).isEqualTo("0");
    }

    @Test
    void paidUserDoesNotSpendFreeQuota() {
        AiUsageLimiter limiter = new AiUsageLimiter(1);
        AiController controller = new AiController(aiService, limiter);
        when(aiService.analyzeJobMatch("Backend", "Java", "Spring")).thenReturn(Map.of("matchScore", 90));

        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map<String, Object>> response = controller.analyzeMatch(
                    Map.of("jobTitle", "Backend", "jobDescription", "Java", "resumeContent", "Spring"),
                    "paid@example.com",
                    "USER",
                    "PRO");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("matchScore", 90);
        }
    }

    @Test
    void blockedRequestDoesNotCallAiService() {
        AiUsageLimiter limiter = new AiUsageLimiter(0);
        AiController controller = new AiController(aiService, limiter);

        ResponseEntity<Map<String, String>> response = controller.generateSummary(
                Map.of("jobTitle", "Java"),
                "free@example.com",
                "FREE",
                "FREE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verifyNoInteractions(aiService);
    }
}
