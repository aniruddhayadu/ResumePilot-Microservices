package com.resumepilot.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final int MAX_ATTEMPTS_PER_ENDPOINT = 2;
	private static final long RETRY_DELAY_MS = 250L;

	@Value("${gemini.url}")
	private String apiUrl;

	@Value("${gemini.key}")
	private String apiKey;

	@Value("${gemini.fallback-urls:}")
	private String fallbackApiUrls;

	@Override
	public String generateSummary(String jobTitle) {
		return generateSummary(jobTitle, "");
	}

	@Override
	public String generateSummary(String jobTitle, String resumeContent) {
		String normalizedTitle = normalizeJobTitle(jobTitle);

		String prompt = String.format(
				"Act as an expert resume writer. Write a concise, professional, ATS-friendly resume summary for a '%s'. "
						+ "Use this candidate context when available and do not invent facts:\n%s\n\n"
						+ "INSTRUCTIONS: Provide ONLY the paragraph text. Do NOT provide multiple options. "
						+ "Do NOT use introductory phrases like 'Here is a summary'. Do NOT use bold markdown or bullet points. "
						+ "Do not invent company names, degrees, years, or fake metrics. "
						+ "Output must be one natural paragraph of 45 to 70 words and must be specific to the role.",
				normalizedTitle,
				summarizeResumeContext(resumeContent));

		return callGemini(prompt);
	}

	@Override
	public String analyzeResume(String jobTitle, String resumeContent) {
		if (resumeContent == null || resumeContent.trim().isEmpty()) {
			return "{\"score\": 0, \"feedback\": \"Resume content is empty. Please fill in your details.\"}";
		}

		String prompt = String.format("Analyze this resume for the role of '%s'.\n\nResume Content:\n%s\n\n"
				+ "Provide a STRICT ATS score out of 100 and brief, actionable feedback. "
				+ "If the uploaded text is not a real resume or is just a normal paragraph, give a low score between 5 and 25. "
				+ "Penalize missing resume sections, missing contact details, weak role-keyword alignment, generic text, and lack of measurable achievements. "
				+ "Reward clear Skills, Experience, Projects, Education, contact details, role-specific keywords, action verbs, and quantified impact. "
				+ "You MUST return ONLY a valid raw JSON object in this exact format: {\"score\": 22, \"feedback\": \"This does not look like a complete resume.\"} "
				+ "Do not add any markdown formatting.", normalizeJobTitle(jobTitle), resumeContent);

	try {
		String result = callGemini(prompt);
		String normalizedResult = normalizeAtsJson(result);
		return normalizedResult.isBlank()
				? fallbackAnalyzeResume(normalizeJobTitle(jobTitle), resumeContent)
				: normalizedResult;
	} catch (Exception e) {
		log.warn("Gemini ATS analysis unavailable; returning local fallback analysis: {}", e.getMessage());
		return fallbackAnalyzeResume(normalizeJobTitle(jobTitle), resumeContent);
	}
	}

	@Override
	public Map<String, Object> analyzeJobMatch(String jobTitle, String jobDescription, String resumeContent) {
		String prompt = String.format(
				"You are an expert ATS (Applicant Tracking System). Compare the following Resume with the Job Description.\n\n"
						+ "Job Title: %s\nJob Description: %s\n\nResume Content:\n%s\n\n"
						+ "Task: Evaluate the fit. You MUST return ONLY a valid raw JSON object in this exact format (no markdown, no quotes outside JSON):\n"
						+ "{\"matchScore\": 85, \"missingSkills\": \"React, Node.js\", \"recommendations\": \"Add project details about React.\"} \n"
						+ "matchScore should be an integer from 0 to 100.",
				jobTitle, jobDescription, resumeContent);

		String jsonResult = callGemini(prompt);

		try {
			return objectMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			log.error("Failed to map AI job-match JSON: {}", e.getMessage());
			return Map.of("matchScore", 0, "missingSkills", "Parse Error", "recommendations",
					"Failed to parse AI response.");
		}
	}

	private String callGemini(String prompt) {
		if (apiKey == null || apiKey.isBlank()) {
			log.error("Gemini API key is not configured. Set GEMINI_API_KEY, GOOGLE_API_KEY, or GEMINI_KEY for ai-service.");
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Gemini API key is not configured for ai-service.");
		}

		for (String endpoint : getGeminiCandidateUrls()) {
			for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_ENDPOINT; attempt++) {
				try {
					log.info("Sending AI request to Gemini endpoint {} (attempt {}/{}).",
							maskGeminiEndpoint(endpoint), attempt, MAX_ATTEMPTS_PER_ENDPOINT);
					String result = postToGemini(endpoint, prompt, expectsJson(prompt));
					if (!result.isBlank()) {
						return result;
					}
					log.warn("Gemini returned empty content from endpoint {}.", maskGeminiEndpoint(endpoint));
					break;
				} catch (RestClientResponseException e) {
					int status = e.getStatusCode().value();
					if (isTransientStatus(status) && attempt < MAX_ATTEMPTS_PER_ENDPOINT) {
						log.warn("Gemini transient error {} from endpoint {}. Retrying once.",
								status, maskGeminiEndpoint(endpoint));
						sleepBeforeRetry(attempt);
						continue;
					}

					if (isTransientStatus(status)) {
						log.warn("Gemini endpoint {} failed with transient status {} after {} attempt(s). Trying next endpoint if configured.",
								maskGeminiEndpoint(endpoint), status, attempt);
						break;
					}

					log.error("Gemini API request failed with status {} from endpoint {}: {}",
							status, maskGeminiEndpoint(endpoint), summarizeError(e.getResponseBodyAsString(), e.getMessage()));
					throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
							"Gemini API request failed: " + summarizeError(e.getResponseBodyAsString(), e.getMessage()));
				} catch (Exception e) {
					if (attempt < MAX_ATTEMPTS_PER_ENDPOINT) {
						log.warn("Gemini request to endpoint {} failed: {}. Retrying once.",
								maskGeminiEndpoint(endpoint), e.getMessage());
						sleepBeforeRetry(attempt);
						continue;
					}
					log.warn("Gemini endpoint {} failed after {} attempt(s): {}",
							maskGeminiEndpoint(endpoint), attempt, e.getMessage());
					break;
				}
			}
		}

		log.warn("All Gemini endpoints failed.");
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"Gemini AI is unavailable right now. Check ai-service logs and API key configuration.");
	}

	private String postToGemini(String endpoint, String prompt, boolean jsonResponse) throws Exception {
		Map<String, Object> generationConfig = jsonResponse
				? Map.of("temperature", 0.15, "maxOutputTokens", 2048)
				: Map.of("temperature", 0.55, "maxOutputTokens", 2048);
		Map<String, Object> requestBody = Map.of(
				"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
				"generationConfig", generationConfig
		);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(appendApiKey(endpoint), entity, String.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "";
		}

		return cleanAiResponse(extractGeminiText(response.getBody()));
	}

	private boolean expectsJson(String prompt) {
		return prompt != null && prompt.toLowerCase(Locale.ROOT).contains("valid raw json object");
	}

	private List<String> getGeminiCandidateUrls() {
		LinkedHashSet<String> endpoints = new LinkedHashSet<>();
		if (apiUrl != null && !apiUrl.isBlank()) {
			endpoints.add(apiUrl.trim());
		}
		if (fallbackApiUrls != null && !fallbackApiUrls.isBlank()) {
			Arrays.stream(fallbackApiUrls.split(","))
					.map(String::trim)
					.filter(value -> !value.isBlank())
					.forEach(endpoints::add);
		}
		return List.copyOf(endpoints);
	}

	private String appendApiKey(String endpoint) {
		String separator = endpoint.contains("?") ? "&" : "?";
		return endpoint + separator + "key=" + apiKey;
	}

	private String extractGeminiText(String responseBody) throws Exception {
		JsonNode root = objectMapper.readTree(responseBody);
		return root.path("candidates")
				.path(0)
				.path("content")
				.path("parts")
				.path(0)
				.path("text")
				.asText("");
	}

	private String cleanAiResponse(String result) {
		if (result == null) {
			return "";
		}

		String cleaned = result.replace("*", "").replace("`", "").replace("#", "").trim();
		String json = extractJsonObject(cleaned);
		if (!json.isBlank()) {
			return json;
		}

		if (cleaned.toLowerCase().contains("option 1")) {
			String[] options = cleaned.split("(?i)Option 2");
			cleaned = options[0].replaceAll("(?i)Option 1[:\\-]*", "").trim();
		}

		if (cleaned.contains("\n")) {
			cleaned = cleaned.lines()
					.map(String::trim)
					.filter(line -> !line.isBlank())
					.findFirst()
					.orElse("");
		}

		return cleaned.trim();
	}

	private String extractJsonObject(String value) {
		int start = value.indexOf("{");
		int end = value.lastIndexOf("}");
		if (start >= 0 && end > start) {
			return value.substring(start, end + 1).trim();
		}
		return "";
	}

	private boolean isTransientStatus(int status) {
		return status == 429 || status == 500 || status == 503 || status == 504;
	}

	private void sleepBeforeRetry(int attempt) {
		try {
			Thread.sleep(RETRY_DELAY_MS * attempt);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String summarizeError(String responseBody, String fallbackMessage) {
		String raw = responseBody == null || responseBody.isBlank() ? fallbackMessage : responseBody;
		if (raw == null) {
			return "No error details returned";
		}
		String compact = raw.replaceAll("\\s+", " ").trim();
		return compact.length() <= 500 ? compact : compact.substring(0, 500);
	}

	private String maskGeminiEndpoint(String endpoint) {
		if (endpoint == null || endpoint.isBlank()) {
			return "unknown";
		}
		int modelIndex = endpoint.indexOf("/models/");
		if (modelIndex >= 0) {
			String model = endpoint.substring(modelIndex + "/models/".length());
			return model.replace(":generateContent", "");
		}
		return endpoint.replaceAll("\\?.*$", "");
	}

	private String summarizeResumeContext(String resumeContent) {
		if (resumeContent == null || resumeContent.isBlank()) {
			return "No candidate context provided.";
		}
		String cleaned = resumeContent.replaceAll("\\s+", " ").trim();
		return cleaned.length() <= 900 ? cleaned : cleaned.substring(0, 900);
	}

	private String normalizeJobTitle(String jobTitle) {
		String normalized = jobTitle == null ? "" : jobTitle.trim().replaceAll("\\s+", " ");
		normalized = normalized.replaceAll("[^a-zA-Z0-9+#. /-]", "").trim();
		if (normalized.isBlank()) {
			return "Professional";
		}
		return Arrays.stream(normalized.split("\\s+"))
				.map(this::toTitleWord)
				.collect(Collectors.joining(" "));
	}

	private String toTitleWord(String word) {
		if (word.length() <= 3 && word.equals(word.toUpperCase(Locale.ROOT))) {
			return word;
		}
		String lower = word.toLowerCase(Locale.ROOT);
		return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
	}

	private String fallbackAnalyzeResume(String jobTitle, String resumeContent) {
		String content = resumeContent == null ? "" : resumeContent;
		String lower = content.toLowerCase(Locale.ROOT);
		int score = 10;

		if (lower.matches("(?s).*\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b.*".toLowerCase(Locale.ROOT))
				|| lower.matches("(?s).*\\b\\d{10}\\b.*")) {
			score += 10;
		}
		if (containsAny(lower, "skills", "technical skills", "technologies")) {
			score += 15;
		}
		if (containsAny(lower, "experience", "work experience", "internship", "employment")) {
			score += 15;
		}
		if (containsAny(lower, "project", "projects")) {
			score += 15;
		}
		if (containsAny(lower, "education", "degree", "b.tech", "bachelor", "university", "college")) {
			score += 10;
		}
		if (containsRoleKeyword(lower, jobTitle)) {
			score += 15;
		}
		if (lower.matches("(?s).*\\b\\d+%\\b.*") || lower.matches("(?s).*\\b\\d+\\+\\b.*")
				|| lower.matches("(?s).*\\b\\d+\\.\\d+\\b.*")) {
			score += 10;
		}
		if (content.trim().split("\\s+").length >= 180) {
			score += 10;
		}

		score = Math.max(5, Math.min(score, 88));
		String feedback = "Gemini is currently unavailable or the configured API key is invalid, so ResumePilot used "
				+ "a local ATS fallback. Score is based on resume structure, contact details, role keywords, projects, "
				+ "education, experience, and measurable impact. Add role-specific keywords for " + jobTitle
				+ ", quantify achievements, and keep clear sections for Skills, Experience, Projects, and Education.";
		return "{\"score\": " + score + ", \"feedback\": \"" + escapeJson(feedback) + "\"}";
	}

	private boolean containsAny(String value, String... candidates) {
		return Arrays.stream(candidates).anyMatch(value::contains);
	}

	private String normalizeAtsJson(String value) {
		try {
			JsonNode root = objectMapper.readTree(value);
			if (root.has("score") && root.has("feedback")
					&& root.path("score").canConvertToInt()
					&& !root.path("feedback").asText("").isBlank()) {
				int score = Math.max(0, Math.min(100, root.path("score").asInt()));
				return "{\"score\": " + score + ", \"feedback\": \""
						+ escapeJson(root.path("feedback").asText()) + "\"}";
			}
		} catch (Exception e) {
			log.warn("Gemini returned invalid ATS JSON, trying repair: {}", value);
		}
		return repairAtsJson(value);
	}

	private String repairAtsJson(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}

		Matcher scoreMatcher = Pattern.compile("\"score\"\\s*:\\s*(\\d+)").matcher(value);
		Matcher feedbackMatcher = Pattern.compile("\"feedback\"\\s*:\\s*\"([\\s\\S]*)").matcher(value);
		if (!scoreMatcher.find() || !feedbackMatcher.find()) {
			return "";
		}

		int score = Math.max(0, Math.min(100, Integer.parseInt(scoreMatcher.group(1))));
		String feedback = feedbackMatcher.group(1)
				.replaceAll("\"\\s*}\\s*$", "")
				.replaceAll("\\s+", " ")
				.trim();
		if (feedback.isBlank()) {
			return "";
		}
		return "{\"score\": " + score + ", \"feedback\": \"" + escapeJson(feedback) + "\"}";
	}

	private boolean containsRoleKeyword(String content, String jobTitle) {
		if (jobTitle == null || jobTitle.isBlank()) {
			return false;
		}
		return Arrays.stream(jobTitle.toLowerCase(Locale.ROOT).split("\\s+"))
				.filter(word -> word.length() > 2)
				.anyMatch(content::contains);
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
