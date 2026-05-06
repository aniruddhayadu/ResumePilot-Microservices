package com.resumepilot.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceImplTest {

    private RestTemplate restTemplate;
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        aiService = new AiServiceImpl(restTemplate);
        ReflectionTestUtils.setField(aiService, "apiUrl", "http://gemini.test/generate");
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
    }

    @Test
    void generateSummaryUsesFallbackWhenJobTitleBlank() {
        String summary = aiService.generateSummary("   ");

        assertThat(summary).contains("Professional");
    }

    @Test
    void analyzeResumeReturnsEmptyContentJsonWithoutCallingAi() {
        String result = aiService.analyzeResume("Java", " ");

        assertThat(result).contains("\"score\": 0");
        assertThat(result).contains("Resume content is empty");
    }

    @Test
    void analyzeResumeExtractsRawJsonFromGeminiMarkdown() {
        mockGeminiResponse("```json\n{\"score\": 88, \"feedback\": \"Solid match\"}\n```");

        String result = aiService.analyzeResume("Java", "Spring Boot resume");

        assertThat(result).isEqualTo("{\"score\": 88, \"feedback\": \"Solid match\"}");
    }

    @Test
    void analyzeJobMatchParsesJsonResponse() {
        mockGeminiResponse("{\"matchScore\": 91, \"missingSkills\": \"Docker\", \"recommendations\": \"Add Docker project\"}");

        Map<String, Object> result = aiService.analyzeJobMatch("Backend", "Needs Docker", "Java Spring");

        assertThat(result).containsEntry("matchScore", 91);
        assertThat(result).containsEntry("missingSkills", "Docker");
    }

    @Test
    void generateSummaryFallsBackWhenGeminiFails() {
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("network down"));

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).contains("Java Developer");
    }

    private void mockGeminiResponse(String text) {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(text) + "}]}}]}";
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    private String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}
