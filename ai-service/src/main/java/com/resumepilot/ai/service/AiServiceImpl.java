package com.resumepilot.ai.service;

import com.resumepilot.ai.dto.ChatReq;
import com.resumepilot.ai.dto.ChatRes;
import com.resumepilot.ai.dto.Msg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

	private final RestTemplate restTemplate;

	@Value("${groq.url}")
	private String apiUrl;

	@Value("${groq.key}")
	private String apiKey;

	@Value("${groq.model}")
	private String model;

	@Override
	public String generateSummary(String jobTitle) {

		if (jobTitle == null || jobTitle.trim().isEmpty()) {
			return getFallbackSummary(jobTitle);
		}

		try {
			log.info("Calling Groq API for: {}", jobTitle);

			// headers
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);

			// prompt
			String prompt = String.format(
					"Write a highly professional 3-sentence resume summary for a %s. Keep it impactful and modern.",
					jobTitle);

			// request body
			ChatReq req = new ChatReq();
			req.setModel(model);
			req.setMessages(List.of(new Msg("system", "You are an expert ATS-friendly resume writer."),
					new Msg("user", prompt)));

			HttpEntity<ChatReq> entity = new HttpEntity<>(req, headers);

			// API call
			ChatRes res = restTemplate.postForObject(apiUrl, entity, ChatRes.class);

			// response parsing
			if (res != null && res.getChoices() != null && !res.getChoices().isEmpty()) {
				Msg msg = res.getChoices().get(0).getMessage();
				if (msg != null && msg.getContent() != null) {
					return msg.getContent().trim();
				}
			}

		} catch (Exception e) {
			log.error("Groq API failed", e);
		}

		return getFallbackSummary(jobTitle);
	}

	private String getFallbackSummary(String jobTitle) {
		String role = (jobTitle != null && !jobTitle.isBlank()) ? jobTitle : "professional";
		return String.format("Results-driven %s with a proven track record of delivering high-quality solutions. "
				+ "Adept at collaborating with cross-functional teams to drive project success. "
				+ "Passionate about continuous learning and implementing best practices.", role);
	}
}