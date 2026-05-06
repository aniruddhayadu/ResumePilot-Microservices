package com.resumepilot.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${gemini.url}")
	private String apiUrl;

	@Value("${gemini.key}")
	private String apiKey;

	@Override
	public String generateSummary(String jobTitle) {
		if (jobTitle == null || jobTitle.trim().isEmpty()) {
			return getFallbackSummary("Professional");
		}

		String prompt = String.format(
				"Act as an expert resume writer. Write a highly professional small and crips resume summary for a '%s'. "
						+ "INSTRUCTIONS: Provide ONLY the paragraph text. Do NOT provide multiple options. "
						+ "Do NOT use introductory phrases like 'Here is a summary'. Do NOT use bold markdown or bullet points. "
						+ "Output must be a single, continuous paragraph.bro i just want simple professional summary, impact full and attractive just summary only,,length ka dhyan rkhoo ",
				jobTitle);

		return callGemini(prompt, getFallbackSummary(jobTitle));
	}

	@Override
	public String analyzeResume(String jobTitle, String resumeContent) {
		if (resumeContent == null || resumeContent.trim().isEmpty()) {
			return "{\"score\": 0, \"feedback\": \"Resume content is empty. Please fill in your details.\"}";
		}
		String prompt = String.format("Analyze this resume for the role of '%s'.\n\nResume Content:\n%s\n\n"
				+ "Provide an ATS score out of 100 and brief feedback. bro do it fairly , ache se i think u have understood "
				+ "You MUST return ONLY a valid raw JSON object in this exact format: {\"score\": 85, \"feedback\": \"Good skills.\"} "
				+ "Do not add any markdown formatting.", jobTitle, resumeContent);

		String fallbackJson = "{\"score\": 70, \"feedback\": \"Could not reach AI for deep analysis.\"}";
		return callGemini(prompt, fallbackJson);
	}

	@Override
	public Map<String, Object> analyzeJobMatch(String jobTitle, String jobDescription, String resumeContent) {
		String prompt = String.format(
				"You are an expert ATS (Applicant Tracking System). Compare the following Resume with the Job Description.\n\n"
						+ "Job Title: %s\nJob Description: %s\n\nResume Content:\n%s\n\n"
						+ "Task: Evaluate the fit. You MUST return ONLY a valid JSON object in this exact format (no markdown, no quotes outside JSON):\n"
						+ "{\"matchScore\": 85, \"missingSkills\": \"React, Node.js\", \"recommendations\": \"Add project details about React.\"} \n"
						+ "matchScore should be an integer from 0 to 100.",
				jobTitle, jobDescription, resumeContent);

		String fallbackJson = "{\"matchScore\": 0, \"missingSkills\": \"Could not analyze\", \"recommendations\": \"AI Service is down.\"}";

		String jsonResult = callGemini(prompt, fallbackJson);

		try {
			return objectMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			log.error("❌ JSON mapping me error aa gaya Job Match ka: {}", e.getMessage());
			return Map.of("matchScore", 0, "missingSkills", "Parse Error", "recommendations",
					"Failed to parse AI response.");
		}
	}

	private String callGemini(String prompt, String fallbackContent) {
		try {
			String fullUrl = apiUrl + "?key=" + apiKey;
			log.info("🚀 AI Request (Gemini 2.5 Flash) bhej raha hoon...");

			Map<String, Object> requestBody = Map.of("contents",
					List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode root = objectMapper.readTree(response.getBody());
				String result = root.path("candidates").get(0).path("content").path("parts").get(0).path("text")
						.asText();

				// STEP 1: Saare Markdown (asterisks, backticks) saaf karo
				result = result.replace("*", "").replace("`", "").replace("#", "").trim();

				// STEP 2: Agar JSON hai, toh sirf JSON part nikalo
				if (result.contains("{") && result.contains("}")) {
					int start = result.indexOf("{");
					int end = result.lastIndexOf("}");
					return result.substring(start, end + 1).trim();
				}

				// STEP 3: Option handling
				if (result.toLowerCase().contains("option 1")) {
					String[] options = result.split("(?i)Option 2");
					result = options[0].replaceAll("(?i)Option 1[:\\-]*", "").trim();
				}

				// STEP 4: First paragraph only
				if (result.contains("\n") && !result.contains("{")) {
					result = result.split("\n")[0];
				}

				return result.trim();
			}
		} catch (Exception e) {
			log.error("❌ Gemini API fail ho gayi: {}", e.getMessage());
		}
		return fallbackContent;
	}

	private String getFallbackSummary(String jobTitle) {
		return String.format(
				"Results-driven %s with a proven track record of delivering high-quality solutions. Adept at collaborating with cross-functional teams to drive project success. Passionate about continuous learning.",
				jobTitle);
	}
}