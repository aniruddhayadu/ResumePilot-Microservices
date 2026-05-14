package com.resumepilot.jobmatch.dto;

import lombok.Data;

@Data
public class JobSearchRequest {
	private String jobTitle;
	private String location;
}