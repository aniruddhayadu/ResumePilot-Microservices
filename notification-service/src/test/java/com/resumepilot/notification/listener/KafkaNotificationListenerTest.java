package com.resumepilot.notification.listener;

import com.resumepilot.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaNotificationListenerTest {

    @Test
    void listenPaymentNotificationsDelegatesToNotificationService() {
        NotificationService notificationService = mock(NotificationService.class);
        KafkaNotificationListener listener = new KafkaNotificationListener(notificationService);

        listener.listenPaymentNotifications("palak@example.com");

        verify(notificationService).sendPaymentSuccessEmail("palak@example.com");
    }
}
