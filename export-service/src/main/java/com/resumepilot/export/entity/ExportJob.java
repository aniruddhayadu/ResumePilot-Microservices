package com.resumepilot.export.entity;

import com.resumepilot.export.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

	@Id
	@Column(length = 36)
	private String jobId;

	private int resumeId;
	private int userId;
	private String format;

	@Enumerated(EnumType.STRING)
	private JobStatus status;

	@Column(length = 500)
	private String fileUrl;

	private long fileSizeKb;

	private LocalDateTime requestedAt;
	private LocalDateTime completedAt;
	private LocalDateTime expiresAt;

	private int templateId;

	@Column(columnDefinition = "TEXT")
	private String customizations;

	@PrePersist
	protected void onCreate() {
		requestedAt = LocalDateTime.now();
		expiresAt = LocalDateTime.now().plusDays(7); // Link expires in 7 days
	}
}