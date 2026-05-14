package com.resumepilot.template.controller;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private PaymentController controller;
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        controller = new PaymentController();
        kafkaTemplate = mock(KafkaTemplate.class);
        ReflectionTestUtils.setField(controller, "kId", "rzp-key");
        ReflectionTestUtils.setField(controller, "kSec", "rzp-secret");
        ReflectionTestUtils.setField(controller, "tEml", "fallback@example.com");
        ReflectionTestUtils.setField(controller, "kTmp", kafkaTemplate);
    }

    @Test
    void createOrderReturnsRazorpayOrderId() throws Exception {
        OrderClient orderClient = mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenReturn(new Order(new JSONObject().put("id", "order_123")));

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class,
                (mock, context) -> mock.orders = orderClient)) {
            ResponseEntity<?> response = controller.crtOrd(Map.of("amount", 49.0));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("order_123");
            ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
            verify(orderClient).create(captor.capture());
            assertThat(captor.getValue().getInt("amount")).isEqualTo(4900);
            assertThat(captor.getValue().getString("currency")).isEqualTo("INR");
        }
    }

    @Test
    void createOrderReturnsServerErrorForBadAmount() {
        ResponseEntity<?> response = controller.crtOrd(Map.of("amount", "bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).asString().contains("Razorpay order creation failed:");
    }

    @Test
    void verifyPaymentSendsProvidedEmailToKafka() {
        ResponseEntity<String> response = controller.vfyPay(Map.of("email", "buyer@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Verified");
        verify(kafkaTemplate).send("notification_topic", "buyer@example.com");
    }

    @Test
    void verifyPaymentFallsBackToConfiguredEmail() {
        ResponseEntity<String> response = controller.vfyPay(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(kafkaTemplate).send("notification_topic", "fallback@example.com");
    }
}
