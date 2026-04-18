package com.resumepilot.auth.dto;
import java.time.LocalDateTime;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {
	private LocalDateTime timestamp;
    private String message;
    private String details;
}
