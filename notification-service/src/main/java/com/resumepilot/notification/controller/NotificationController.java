package com.resumepilot.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification System", description = "Endpoints for Notification Service")
public class NotificationController {

	@GetMapping("/health")
	@Operation(summary = "Check Health", description = "Checks if service is UP")
	public ResponseEntity<String> checkHealth() {
		return ResponseEntity.ok(" Notification Service is UP & running...");
	}
}