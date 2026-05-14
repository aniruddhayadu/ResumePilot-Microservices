package com.resumepilot.export.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResumeDto {
	private Long id;
	private String title;
	private String userEmail;
	private String content;
	private LocalDateTime createdAt;
}