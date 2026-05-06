package com.resumepilot.ai.service;

import java.util.Map;

public interface AiService {
	String generateSummary(String jobTitle);

	String analyzeResume(String jobTitle, String resumeContent);

	Map<String, Object> analyzeJobMatch(String jobTitle, String jobDescription, String resumeContent);
}