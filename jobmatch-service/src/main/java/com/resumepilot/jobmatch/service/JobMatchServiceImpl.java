package com.resumepilot.jobmatch.service;

import com.resumepilot.jobmatch.entity.JobMatch;
import com.resumepilot.jobmatch.repository.JobMatchRepository;
import com.resumepilot.jobmatch.client.AiServiceClient; // 🚀 Naya Import
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

	private final JobMatchRepository repository;
	private final RestTemplate restTemplate;
	private final AiServiceClient aiServiceClient; // 🚀 Inject kiya

	@Value("${api.rapidapi.key}")
	private String apiKey;

	@Value("${api.rapidapi.host}")
	private String apiHost;

	@Value("${api.rapidapi.url}")
	private String apiUrl;

	@Override
	public JobMatch analyzeJobFit(int resumeId, int userId, String jobTitle, String jobDescription,
			String resumeContent) {
		JobMatch match = new JobMatch();
		match.setResumeId(resumeId);
		match.setUserId(userId);
		match.setJobTitle(jobTitle);
		match.setJobDescription(jobDescription);
		match.setSource("LINKEDIN");

		try {
			// 🚀 AI ko bhejne ke liye request payload banana
			Map<String, Object> aiRequest = new HashMap<>();
			aiRequest.put("jobTitle", jobTitle);
			aiRequest.put("jobDescription", jobDescription);
			aiRequest.put("resumeContent", resumeContent);

			// 🚀 FeignClient se direct AI-Service call kar rahe hain
			Map<String, Object> aiResponse = aiServiceClient.getMatchScoreFromAI(aiRequest);

			// 🚀 AI ka response apne database entity mein save kar rahe hain
			match.setMatchScore((Integer) aiResponse.getOrDefault("matchScore", 0));
			match.setMissingSkills((String) aiResponse.getOrDefault("missingSkills", "Analysis Failed"));
			match.setRecommendations((String) aiResponse.getOrDefault("recommendations", "No recommendations"));

		} catch (Exception e) {
			System.err.println("❌ AI Service Error: " + e.getMessage());
			match.setMatchScore(0);
			match.setMissingSkills("Could not connect to AI Service");
			match.setRecommendations("Make sure ai-service is running on Eureka.");
		}

		return repository.save(match);
	}

	@Override
	public List<Map<String, Object>> fetchJobsFromLinkedIn(String jobTitle, String location) {
		try {
			String query = jobTitle + " in " + location;
			String url = apiUrl + "?query=" + query + "&page=1&num_pages=1";

			HttpHeaders headers = new HttpHeaders();
			headers.set("X-RapidAPI-Key", apiKey);
			headers.set("X-RapidAPI-Host", apiHost);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.getBody());
			JsonNode dataNode = root.get("data");

			List<Map<String, Object>> jobsList = new ArrayList<>();
			if (dataNode != null && dataNode.isArray()) {
				for (JsonNode job : dataNode) {
					Map<String, Object> jobMap = new HashMap<>();
					jobMap.put("title", job.get("job_title").asText("N/A"));
					jobMap.put("company", job.get("employer_name").asText("N/A"));
					jobMap.put("location", job.has("job_city") ? job.get("job_city").asText() : location);
					jobMap.put("url", job.has("job_apply_link") ? job.get("job_apply_link").asText() : "");
					jobsList.add(jobMap);
				}
			}
			return jobsList;

		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	@Override
	public List<Map<String, Object>> fetchJobsFromNaukri(String jobTitle, String location) {
		return fetchJobsFromLinkedIn(jobTitle, location);
	}

	@Override
	public List<JobMatch> getMatchesByResume(int resumeId) {
		return repository.findByResumeId(resumeId);
	}

	@Override
	public List<JobMatch> getMatchesByUser(int userId) {
		return repository.findByUserId(userId);
	}

	@Override
	public Optional<JobMatch> getMatchById(int matchId) {
		return repository.findByMatchId(matchId);
	}

	@Override
	public void bookmarkMatch(int matchId) {
		JobMatch match = repository.findById(matchId).orElseThrow();
		match.setBookmarked(!match.isBookmarked());
		repository.save(match);
	}

	@Override
	public String getTailoringRecommendations(int matchId, String jobDescription) {
		return "Tailoring recommendations handled by AI.";
	}

	@Override
	public void deleteMatch(int matchId) {
		repository.deleteById(matchId);
	}

	@Override
	public List<JobMatch> getTopMatches(int resumeId) {
		return repository.findByMatchScoreGreaterThan(75);
	}
}