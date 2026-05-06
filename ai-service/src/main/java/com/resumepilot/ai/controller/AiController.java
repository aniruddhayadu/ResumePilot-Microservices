package com.resumepilot.ai.controller;

import com.resumepilot.ai.service.AiService;
import com.resumepilot.ai.service.AiUsageLimiter;
import com.resumepilot.ai.service.AiUsageLimiter.UsageDecision;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiController {

    private final AiService aiService;
    private final AiUsageLimiter usageLimiter;

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, String>> generateSummary(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "User-Email", required = false) String userEmail,
            @RequestHeader(value = "User-Role", required = false) String userRole,
            @RequestHeader(value = "Subscription-Plan", required = false) String subscriptionPlan) {
        return withAiLimit(userEmail, userRole, subscriptionPlan, () -> {
            String jobTitle = request.getOrDefault("jobTitle", "Professional");
            String result = aiService.generateSummary(jobTitle);
            return ResponseEntity.ok(Map.of("generatedSummary", result));
        });
    }

    @PostMapping("/analyze-ats")
    public ResponseEntity<String> analyzeResume(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "User-Email", required = false) String userEmail,
            @RequestHeader(value = "User-Role", required = false) String userRole,
            @RequestHeader(value = "Subscription-Plan", required = false) String subscriptionPlan) {
        return withAiLimit(userEmail, userRole, subscriptionPlan, () -> {
            String result = aiService.analyzeResume(request.get("jobTitle"), request.get("resumeContent"));
            return ResponseEntity.ok(result);
        });
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
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "User-Email", required = false) String userEmail,
            @RequestHeader(value = "User-Role", required = false) String userRole,
            @RequestHeader(value = "Subscription-Plan", required = false) String subscriptionPlan) {
        return withAiLimit(userEmail, userRole, subscriptionPlan, () -> {
            String jobTitle = (String) request.getOrDefault("jobTitle", "N/A");
            String jobDescription = (String) request.getOrDefault("jobDescription", "N/A");
            String resumeContent = (String) request.getOrDefault("resumeContent", "No resume text provided");

            Map<String, Object> result = aiService.analyzeJobMatch(jobTitle, jobDescription, resumeContent);
            return ResponseEntity.ok(result);
        });
    }

    private <T> ResponseEntity<T> withAiLimit(
            String userEmail,
            String userRole,
            String subscriptionPlan,
            Supplier<ResponseEntity<T>> action) {
        UsageDecision decision = usageLimiter.tryConsume(userEmail, userRole, subscriptionPlan);
        if (!decision.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-AI-Free-Limit", String.valueOf(decision.limit()))
                    .header("X-AI-Remaining", String.valueOf(decision.remaining()))
                    .body(null);
        }

        ResponseEntity<T> response = action.get();
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header("X-AI-Free-Limit", String.valueOf(decision.limit()))
                .header("X-AI-Remaining", String.valueOf(decision.remaining()))
                .body(response.getBody());
    }
}
