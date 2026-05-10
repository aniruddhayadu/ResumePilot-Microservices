package com.resumepilot.notification.listener;

import com.resumepilot.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaNotificationListener {

	// 🚀 Dekh bhai, humne Interface inject kiya hai, implementation nahi! (Loose
	// Coupling)
	private final NotificationService notificationService;

	@KafkaListener(topics = "notification_topic", groupId = "notification-group")
	public void listenPaymentNotifications(String userEmail) {
		System.out.println("📥 Kafka received message for: " + userEmail);
		notificationService.sendPaymentSuccessEmail(userEmail);
	}
	@KafkaListener(topics = "auth_otp_topic", groupId = "notification-group")
	public void listenOtpNotifications(String payload) {
		String[] parts = payload.split("\\|", 2);
		if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
			System.err.println("Invalid OTP Kafka payload received");
			return;
		}
		notificationService.sendOtpEmail(parts[0].trim(), parts[1].trim());
	}
}
