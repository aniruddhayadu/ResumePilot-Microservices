package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @Test
    void generateSummaryCallsAiServiceDirectly() {
        AiController controller = controller();
        when(aiService.generateSummary("Java Developer", "Skills: Java")).thenReturn("Strong Java summary");

        ResponseEntity<Map<String, String>> response = controller.generateSummary(
                Map.of("jobTitle", "Java Developer", "resumeContent", "Skills: Java"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("generatedSummary", "Strong Java summary");
        verify(aiService).generateSummary("Java Developer", "Skills: Java");
    }

    @Test
    void analyzeAtsCallsAiServiceDirectly() {
        AiController controller = controller();
        when(aiService.analyzeResume("Java", "resume text")).thenReturn("{\"score\":80,\"feedback\":\"Good\"}");

        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> response = controller.analyzeResume(
                    Map.of("jobTitle", "Java", "resumeContent", "resume text"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"score\":80");
        }
    }

    @Test
    void analyzeMatchUsesJobMatchAiPath() {
        AiController controller = controller();
        when(aiService.analyzeJobMatch("Backend", "Java", "Spring")).thenReturn(Map.of("matchScore", 90));

        ResponseEntity<Map<String, Object>> response = controller.analyzeMatch(
                Map.of("jobTitle", "Backend", "jobDescription", "Java", "resumeContent", "Spring"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("matchScore", 90);
        verify(aiService).analyzeJobMatch("Backend", "Java", "Spring");
    }

    private AiController controller() {
        return new AiController(aiService);
    }
}
