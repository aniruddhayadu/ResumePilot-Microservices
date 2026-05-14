package com.resumepilot.resume.service;

import com.resumepilot.resume.entity.Resume;
import com.resumepilot.resume.exception.ResourceNotFoundException;
import com.resumepilot.resume.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

	private final ResumeRepository repository;

	@Autowired
	public ResumeServiceImpl(ResumeRepository repository) {
		this.repository = repository;
	}

	@Override
	public Resume saveResume(Resume resume) {
		return repository.save(resume);
	}

	@Override
	public List<Resume> getResumesByEmail(String email) {
		return repository.findByUserEmail(email);
	}

	@Override
	public Resume getResumeById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));
	}

	@Override
	public void deleteResume(Long id) {
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("Cannot delete. Resume not found with id: " + id);
		}
		repository.deleteById(id);
	}
}