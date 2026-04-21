package com.resumepilot.resume.repository;

import com.resumepilot.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
	List<Resume> findByUserEmail(String email);
}