package com.resumepilot.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
	private String email;
	private String password;
}
