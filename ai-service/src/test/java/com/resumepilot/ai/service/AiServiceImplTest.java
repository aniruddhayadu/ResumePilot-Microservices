package com.resumepilot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
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
    void analyzeResumeReturnsEmptyContentJsonWhenResumeContentIsNull() {
        String result = aiService.analyzeResume("Java", null);

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
    void analyzeJobMatchReturnsParseErrorWhenAiResponseIsNotJson() {
        mockGeminiResponse("This is not JSON");

        Map<String, Object> result = aiService.analyzeJobMatch("Backend", "Needs Docker", "Java Spring");

        assertThat(result).containsEntry("matchScore", 0);
        assertThat(result).containsEntry("missingSkills", "Parse Error");
        assertThat(result).containsEntry("recommendations", "Failed to parse AI response.");
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
    void generateSummaryMapsNonTransientGeminiErrorsToBadGateway() {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "bad request",
                HttpHeaders.EMPTY,
                "  Gemini rejected the prompt  ".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(badRequest);

        assertThatThrownBy(() -> aiService.generateSummary("Java Developer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502 BAD_GATEWAY")
                .hasMessageContaining("Gemini rejected the prompt");
    }

    @Test
    void generateSummaryTruncatesLongGeminiErrorDetails() {
        String longError = "x".repeat(600);
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "bad request",
                HttpHeaders.EMPTY,
                longError.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenThrow(badRequest);

        assertThatThrownBy(() -> aiService.generateSummary("Java Developer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502 BAD_GATEWAY")
                .hasMessageContaining("x".repeat(100));
    }

    @Test
    void generateSummaryTreatsNonSuccessfulResponseAsUnavailable() {
        when(restTemplate.postForEntity(eq("http://gemini.test/generate?key=test-key"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));

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
    void generateSummaryUsesFallbackWhenPrimaryUrlIsNullAndFallbackListHasBlankEntries() {
        ReflectionTestUtils.setField(aiService, "apiUrl", null);
        ReflectionTestUtils.setField(aiService, "fallbackApiUrls", " , http://gemini.test/fallback , ");
        mockGeminiResponse("http://gemini.test/fallback?key=test-key", "Fallback summary");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("Fallback summary");
    }

    @Test
    void generateSummaryFailsWhenNoCandidateUrlsAreConfigured() {
        ReflectionTestUtils.setField(aiService, "apiUrl", " ");
        ReflectionTestUtils.setField(aiService, "fallbackApiUrls", null);

        assertThatThrownBy(() -> aiService.generateSummary("Java Developer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gemini AI is unavailable");
    }

    @Test
    void generateSummarySupportsEndpointsThatAlreadyHaveQueryParameters() {
        ReflectionTestUtils.setField(aiService, "apiUrl", "http://gemini.test/generate?model=flash");
        mockGeminiResponse("http://gemini.test/generate?model=flash&key=test-key", "Generated summary from Gemini");

        String result = aiService.generateSummary("QA");

        assertThat(result).isEqualTo("Generated summary from Gemini");
    }

    @Test
    void generateSummarySupportsGeminiModelEndpointShape() {
        ReflectionTestUtils.setField(
                aiService,
                "apiUrl",
                "http://gemini.test/v1beta/models/gemini-1.5-flash:generateContent");
        mockGeminiResponse(
                "http://gemini.test/v1beta/models/gemini-1.5-flash:generateContent?key=test-key",
                "Generated summary from Gemini");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("Generated summary from Gemini");
    }

    @Test
    void generateSummaryHandlesNullTitleAndNullResumeContext() {
        mockGeminiResponse("Generated professional summary");

        String result = aiService.generateSummary(null, null);

        assertThat(result).isEqualTo("Generated professional summary");
    }

    @Test
    void generateSummaryAcceptsLongResumeContext() {
        mockGeminiResponse("Generated long-context summary");

        String result = aiService.generateSummary("Java Developer", "Java ".repeat(300));

        assertThat(result).isEqualTo("Generated long-context summary");
    }

    @Test
    void generateSummaryKeepsMalformedJsonLookingTextAsPlainText() {
        mockGeminiResponse("{unfinished");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("{unfinished");
    }

    @Test
    void generateSummaryKeepsFirstOptionWhenGeminiReturnsMultipleOptions() {
        mockGeminiResponse("Option 1: First polished summary.\nOption 2: Second summary.");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("First polished summary.");
    }

    @Test
    void generateSummaryKeepsFirstNonBlankLineFromMultiLineResponse() {
        mockGeminiResponse("\n\nFirst usable summary.\nSecond line should be ignored.");

        String result = aiService.generateSummary("Java Developer");

        assertThat(result).isEqualTo("First usable summary.");
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

    @Test
    void missingNullGeminiKeyFailsClearly() {
        ReflectionTestUtils.setField(aiService, "apiKey", null);

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
