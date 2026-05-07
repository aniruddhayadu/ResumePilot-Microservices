package com.resumepilot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceImplTest {

    private RestTemplate restTemplate;
    private AiServiceImpl aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        aiService = new AiServiceImpl(restTemplate);
        ReflectionTestUtils.setField(aiService, "apiUrl", "http://gemini.test/generate");
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "fallbackApiUrls", "");
    }

    @Test
    void generateSummaryUsesGeminiEvenWhenJobTitleBlank() {
        mockGeminiResponse("Professional summary from Gemini");

        String summary = aiService.generateSummary("   ");

        assertThat(summary).isEqualTo("Professional summary from Gemini");
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

        String result = aiService.analyzeResume("Java Developer", javaResumeText());

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
    void generateSummaryFailsClearlyWhenGeminiFails() {
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> aiService.generateSummary("Java Developer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gemini AI is unavailable");
    }

    @Test
    void generateSummarySendsUnusualTitleToGemini() {
        mockGeminiResponse("Generated summary from Gemini");

        String result = aiService.generateSummary("ijgi agdcjksif ba");

        assertThat(result).isEqualTo("Generated summary from Gemini");
    }

    @Test
    void generateSummaryRetriesThenUsesFallbackEndpointWhenPrimaryIsUnavailable() {
        ReflectionTestUtils.setField(aiService, "fallbackApiUrls", "http://gemini.test/fallback");
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "high demand"));
        mockGeminiResponse("http://gemini.test/fallback?key=test-key", "Reliable Java summary");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("Reliable Java summary");
        verify(restTemplate, times(2)).postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class));
        verify(restTemplate).postForEntity(eq("http://gemini.test/fallback?key=test-key"), any(), eq(String.class));
    }

    @Test
    void analyzeResumeFailsClearlyWhenGeminiIsUnavailable() {
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> aiService.analyzeResume("Spring Boot Developer", javaResumeText()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gemini AI is unavailable");
    }

    @Test
    void analyzeResumeSendsPlainNonResumeTextToGemini() throws Exception {
        mockGeminiResponse("{\"score\": 12, \"feedback\": \"This does not look like a complete resume.\"}");

        String result = aiService.analyzeResume(
                "Java Developer",
                "This is a normal paragraph from a random PDF. It talks about college events, lunch plans, and a few unrelated notes. It is not a resume and has no skills, projects, education, contact details, or work experience.");

        JsonNode json = objectMapper.readTree(result);
        assertThat(json.path("score").asInt()).isLessThanOrEqualTo(25);
        assertThat(json.path("feedback").asText()).contains("does not look like a complete resume");
    }

    @Test
    void missingGeminiKeyFailsClearly() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        assertThatThrownBy(() -> aiService.generateSummary("Java Developer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    private void mockGeminiResponse(String text) {
        mockGeminiResponse("http://gemini.test/generate?key=test-key", text);
    }

    private void mockGeminiResponse(String url, String text) {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(text) + "}]}}]}";
        when(restTemplate.postForEntity(eq(url), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    private String javaResumeText() {
        return """
                John Doe
                john.doe@example.com | +91 9876543210 | linkedin.com/in/johndoe
                Summary
                Java Developer with hands-on experience building REST APIs and backend services.
                Skills
                Java, Spring Boot, MySQL, REST API, Git, Docker, JUnit, Microservices
                Experience
                Developed Spring Boot APIs, implemented database integrations, tested services, and improved API response time by 30 percent.
                Projects
                Built a resume platform using Java, Spring Boot, React, MySQL, and JWT authentication.
                Education
                Bachelor of Technology in Computer Science
                """;
    }

    private String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}
