package com.resumepilot.auth.dto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class AuthResponse {
	private String token;
	private String role;
	private String fullName;
}
