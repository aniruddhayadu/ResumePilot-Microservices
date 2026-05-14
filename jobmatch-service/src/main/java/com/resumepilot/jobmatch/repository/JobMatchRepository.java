package com.resumepilot.jobmatch.repository;

import com.resumepilot.jobmatch.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, Integer> {
	List<JobMatch> findByResumeId(int resumeId);

	List<JobMatch> findByUserId(int userId);

	Optional<JobMatch> findByMatchId(int matchId);

	List<JobMatch> findByMatchScoreGreaterThan(int score);

	List<JobMatch> findByIsBookmarked(boolean isBookmarked);

	List<JobMatch> findByJobTitle(String jobTitle);

	int countByUserId(int userId);
}