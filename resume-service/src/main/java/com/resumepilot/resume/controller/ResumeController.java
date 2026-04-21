package com.resumepilot.resume.controller;

import com.resumepilot.resume.entity.Resume;
import com.resumepilot.resume.repository.ResumeRepository;
import com.resumepilot.resume.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume")
public class ResumeController {

	private final ResumeService resumeService;
	private final ResumeRepository resumeRepository; // Add this line

	@Autowired
	public ResumeController(ResumeService resumeService, ResumeRepository resumeRepository) {
		this.resumeService = resumeService;
		this.resumeRepository = resumeRepository; // Initialize it
	}

	@PostMapping("/create")
	public ResponseEntity<Resume> createResume(@RequestBody Resume resume, @RequestHeader("User-Email") String email) {
		resume.setUserEmail(email);
		return ResponseEntity.ok(resumeService.saveResume(resume));
	}

	@GetMapping("/my-resumes")
	public ResponseEntity<List<Resume>> getMyResumes(@RequestHeader("User-Email") String email) {
		return ResponseEntity.ok(resumeService.getResumesByEmail(email));
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Map<String, String>> deleteResume(@PathVariable Long id) {
		resumeService.deleteResume(id);
		Map<String, String> response = new HashMap<>();
		response.put("message", "Resume deleted successfully");
		return ResponseEntity.ok(response);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<?> updateResume(@PathVariable("id") Long id, @RequestBody Resume updatedResume,
			@RequestHeader("User-Email") String email) {

		return resumeRepository.findById(id).map(existingResume -> {
			if (!existingResume.getUserEmail().equals(email)) {
				return ResponseEntity.status(403).body("Unauthorized edit attempt");
			}
			existingResume.setTitle(updatedResume.getTitle());
			existingResume.setContent(updatedResume.getContent());
			resumeService.saveResume(existingResume);
			return ResponseEntity.ok(existingResume);
		}).orElse(ResponseEntity.notFound().build());
	}
}