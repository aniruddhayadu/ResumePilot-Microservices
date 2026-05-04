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
}