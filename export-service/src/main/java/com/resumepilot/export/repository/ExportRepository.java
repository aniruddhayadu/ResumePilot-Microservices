package com.resumepilot.export.repository;

import com.resumepilot.export.entity.ExportJob;
import com.resumepilot.export.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportRepository extends JpaRepository<ExportJob, String> {

	List<ExportJob> findByUserId(int userId);

	List<ExportJob> findByResumeId(int resumeId);

	List<ExportJob> findByStatus(JobStatus status);

	List<ExportJob> findByFormat(String format);

	@Query("SELECT e FROM ExportJob e WHERE e.expiresAt < :now")
	List<ExportJob> findExpiredJobs(LocalDateTime now);

	@Query("SELECT COUNT(e) FROM ExportJob e WHERE e.userId = :userId AND DATE(e.requestedAt) = CURRENT_DATE")
	int countByUserIdToday(int userId);
}