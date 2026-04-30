package com.resumepilot.ai.service;

public interface AiService {
	String generateSummary(String jobTitle);

	String analyzeResume(String jobTitle, String resumeContent);
}