package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin("*") // connecting react
public class AiController {

	private final AiService aiService;

	@PostMapping("/generate-summary")
	public ResponseEntity<Map<String, String>> generateSummary(@RequestBody Map<String, String> request) {
		String jobTitle = request.get("jobTitle");

		// service calling
		String generatedText = aiService.generateSummary(jobTitle);

		return ResponseEntity.ok(Map.of("generatedSummary", generatedText));
	}
}