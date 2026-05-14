package com.resumepilot.jobmatch.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches")
@Data
public class JobMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int matchId;

	private int resumeId;
	private int userId;

	private String jobTitle;

	@Column(columnDefinition = "TEXT")
	private String jobDescription;

	private int matchScore; // 0-100

	@Column(columnDefinition = "TEXT")
	private String missingSkills; // comma-separated

	@Column(columnDefinition = "TEXT")
	private String recommendations;

	private String source; // LINKEDIN / NAUKRI / MANUAL

	private LocalDateTime matchedAt;

	private boolean isBookmarked;

	@PrePersist
	protected void onCreate() {
		this.matchedAt = LocalDateTime.now();
		this.isBookmarked = false;
	}
}