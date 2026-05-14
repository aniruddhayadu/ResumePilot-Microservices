package com.resumepilot.resume.service;

import com.resumepilot.resume.entity.Resume;
import java.util.List;

public interface ResumeService {
	Resume saveResume(Resume resume);

	List<Resume> getResumesByEmail(String email);

	Resume getResumeById(Long id);

	void deleteResume(Long id);
}