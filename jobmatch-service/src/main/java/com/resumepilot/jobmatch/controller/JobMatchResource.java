package com.resumepilot.jobmatch.controller;

import com.resumepilot.jobmatch.entity.JobMatch;
import com.resumepilot.jobmatch.service.JobMatchService;
import com.resumepilot.jobmatch.dto.JobSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/job-matches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Job Match Controller", description = "APIs for fetching jobs and analyzing resume fit")
public class JobMatchResource {

	private final JobMatchService jobMatchService;

	@PostMapping("/analyze/{resumeId}")
	@Operation(summary = "Analyze resume fit for a specific job")
	public ResponseEntity<JobMatch> analyze(@PathVariable int resumeId, @RequestBody Map<String, Object> payload) {
		int userId = (Integer) payload.getOrDefault("userId", 0);
		String jobTitle = (String) payload.getOrDefault("jobTitle", "N/A");
		String jobDescription = (String) payload.getOrDefault("jobDescription", "");
		// 🚀 Frontend se resume content receive karenge
		String resumeContent = (String) payload.getOrDefault("resumeContent", "No resume content");

		return ResponseEntity
				.ok(jobMatchService.analyzeJobFit(resumeId, userId, jobTitle, jobDescription, resumeContent));
	}

	@GetMapping("/byResume/{resumeId}")
	@Operation(summary = "Get all job matches by Resume ID")
	public ResponseEntity<List<JobMatch>> getByResume(@PathVariable int resumeId) {
		return ResponseEntity.ok(jobMatchService.getMatchesByResume(resumeId));
	}

	@GetMapping("/byUser/{userId}")
	@Operation(summary = "Get all job matches by User ID")
	public ResponseEntity<List<JobMatch>> getByUser(@PathVariable int userId) {
		return ResponseEntity.ok(jobMatchService.getMatchesByUser(userId));
	}

	@GetMapping("/{matchId}")
	@Operation(summary = "Get a specific job match by its ID")
	public ResponseEntity<Optional<JobMatch>> getById(@PathVariable int matchId) {
		return ResponseEntity.ok(jobMatchService.getMatchById(matchId));
	}

	@PostMapping("/bookmark/{matchId}")
	@Operation(summary = "Bookmark or unbookmark a job match")
	public ResponseEntity<String> bookmark(@PathVariable int matchId) {
		jobMatchService.bookmarkMatch(matchId);
		return ResponseEntity.ok("Match bookmarked successfully");
	}

	@PostMapping("/fetchLinkedIn")
	@Operation(summary = "Fetch real-time jobs from LinkedIn via JSearch")
	public ResponseEntity<List<Map<String, Object>>> fetchLinkedIn(@RequestBody JobSearchRequest request) {
		return ResponseEntity.ok(jobMatchService.fetchJobsFromLinkedIn(request.getJobTitle(), request.getLocation()));
	}

	@PostMapping("/fetchNaukri")
	@Operation(summary = "Fetch real-time jobs from Naukri (via JSearch)")
	public ResponseEntity<List<Map<String, Object>>> fetchNaukri(@RequestBody JobSearchRequest request) {
		return ResponseEntity.ok(jobMatchService.fetchJobsFromNaukri(request.getJobTitle(), request.getLocation()));
	}
}