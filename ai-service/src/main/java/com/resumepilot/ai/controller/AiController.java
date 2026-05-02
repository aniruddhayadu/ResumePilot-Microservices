package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiController {

	private final AiService aiService;

	@PostMapping("/generate-summary")
	public ResponseEntity<Map<String, String>> generateSummary(@RequestBody Map<String, String> request) {
		String jobTitle = request.getOrDefault("jobTitle", "Professional");
		String result = aiService.generateSummary(jobTitle);
		return ResponseEntity.ok(Map.of("generatedSummary", result));
	}

	@PostMapping("/analyze-ats")
	public ResponseEntity<String> analyzeResume(@RequestBody Map<String, String> request) {
		String result = aiService.analyzeResume(request.get("jobTitle"), request.get("resumeContent"));
		return ResponseEntity.ok(result);
	}

	@PostMapping("/extract-pdf")
	public ResponseEntity<Map<String, String>> extractPdfText(@RequestParam("file") MultipartFile file) {
		try (PDDocument document = PDDocument.load(file.getInputStream())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return ResponseEntity.ok(Map.of("text", stripper.getText(document)));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid PDF"));
		}
	}

}