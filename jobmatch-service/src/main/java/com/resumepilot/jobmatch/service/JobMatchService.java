package com.resumepilot.jobmatch.service;

import com.resumepilot.jobmatch.entity.JobMatch;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JobMatchService {
	JobMatch analyzeJobFit(int resumeId, int userId, String jobTitle, String jobDescription, String resumeContent);

	List<JobMatch> getMatchesByResume(int resumeId);

	List<JobMatch> getMatchesByUser(int userId);

	Optional<JobMatch> getMatchById(int matchId);

	void bookmarkMatch(int matchId);

	List<Map<String, Object>> fetchJobsFromLinkedIn(String jobTitle, String location);

	List<Map<String, Object>> fetchJobsFromNaukri(String jobTitle, String location);

	String getTailoringRecommendations(int matchId, String jobDescription);

	void deleteMatch(int matchId);

	List<JobMatch> getTopMatches(int resumeId);
}