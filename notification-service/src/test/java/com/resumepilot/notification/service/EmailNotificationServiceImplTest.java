package com.resumepilot.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationServiceImpl service;

    @Test
    void sendPaymentSuccessEmailBuildsExpectedMail() {
        ReflectionTestUtils.setField(service, "senderEmail", "noreply@resumepilot.com");

        service.sendPaymentSuccessEmail("palak@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@resumepilot.com");
        assertThat(message.getTo()).containsExactly("palak@example.com");
        assertThat(message.getSubject()).contains("Premium Template");
        assertThat(message.getText()).contains("payment successful");
    }

    @Test
    void sendPaymentSuccessEmailSwallowsMailFailures() {
        ReflectionTestUtils.setField(service, "senderEmail", "noreply@resumepilot.com");
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatCode(() -> service.sendPaymentSuccessEmail("palak@example.com")).doesNotThrowAnyException();
    }
}
