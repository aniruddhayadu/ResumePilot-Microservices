package com.resumepilot.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class NotificationServiceApplicationTests {

	@MockBean
	private JavaMailSender javaMailSender;

	@MockBean
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	@Test
	void contextLoads() {
	}
}