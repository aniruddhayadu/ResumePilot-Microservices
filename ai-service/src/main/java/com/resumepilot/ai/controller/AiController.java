package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, String>> generateSummary(
            @RequestBody Map<String, String> request) {
        String jobTitle = request.getOrDefault("jobTitle", "Professional");
        String resumeContent = request.getOrDefault("resumeContent", "");
        try {
            String result = aiService.generateSummary(jobTitle, resumeContent);
            return ResponseEntity.ok(Map.of("generatedSummary", result));
        } catch (Exception e) {
            log.warn("Gemini summary generation failed; returning local fallback: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("generatedSummary", localSummaryFallback(jobTitle)));
        }
    }

    @PostMapping("/analyze-ats")
    public ResponseEntity<String> analyzeResume(
            @RequestBody Map<String, String> request) {
        try {
            String result = aiService.analyzeResume(request.get("jobTitle"), request.get("resumeContent"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Gemini ATS analysis failed; returning local fallback: {}", e.getMessage());
            return ResponseEntity.ok(localAtsFallback(request));
        }
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

    @PostMapping("/analyze-match")
    public ResponseEntity<Map<String, Object>> analyzeMatch(
            @RequestBody Map<String, Object> request) {
        String jobTitle = (String) request.getOrDefault("jobTitle", "N/A");
        String jobDescription = (String) request.getOrDefault("jobDescription", "N/A");
        String resumeContent = (String) request.getOrDefault("resumeContent", "No resume text provided");

        try {
            Map<String, Object> result = aiService.analyzeJobMatch(jobTitle, jobDescription, resumeContent);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Gemini job-match analysis failed; returning local fallback: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "matchScore", 60,
                    "missingSkills", "Local Fallback",
                    "recommendations", "Local Fallback: Gemini AI is unavailable or the API key is invalid. Add role keywords from the job description and quantify your resume achievements."));
        }
    }

    private String localSummaryFallback(String jobTitle) {
        String role = jobTitle == null || jobTitle.isBlank() ? "the target role" : jobTitle.trim();
        return "Local Fallback: Gemini AI is unavailable or the API key is invalid. Create a concise, ATS-friendly summary for "
                + role + " using your strongest skills, projects, experience, and measurable achievements.";
    }

    private String localAtsFallback(Map<String, String> request) {
        String resumeContent = request.getOrDefault("resumeContent", "");
        int score = resumeContent == null || resumeContent.isBlank() ? 20 : 62;
        String feedback = "Local Fallback: Gemini AI is unavailable or the API key is invalid, so ResumePilot generated a dummy ATS score locally. Add contact details, Skills, Experience, Projects, Education, role-specific keywords, and quantified achievements for a stronger score.";
        return "{\"score\": " + score + ", \"feedback\": \"" + escapeJson(feedback) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
