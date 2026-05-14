package com.resumepilot.template;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.resumepilot.template.entity.Template;
import com.resumepilot.template.repository.TemplateRepository;
import com.resumepilot.template.service.TemplateServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository repo;

    @InjectMocks
    private TemplateServiceImpl service;

    private Template template;

    @BeforeEach
    void setup() {
        template = new Template();
        template.setId(1L);
        template.setName("Pro ATS");
        template.setIsPremium(true);
        template.setPrice(49.0);
    }

    @Test
    void getAllTemplatesReturnsRepositoryResults() {
        when(repo.findAll()).thenReturn(List.of(template));

        List<Template> result = service.getAllTemplates();

        assertThat(result).containsExactly(template);
        verify(repo).findAll();
    }

    @Test
    void saveTemplatePersistsTemplate() {
        when(repo.save(any(Template.class))).thenReturn(template);

        Template result = service.saveTemplate(template);

        assertThat(result).isSameAs(template);
        verify(repo).save(template);
    }

    @Test
    void getTemplateByIdReturnsTemplateOrThrows() {
        when(repo.findById(1L)).thenReturn(Optional.of(template));
        when(repo.findById(2L)).thenReturn(Optional.empty());

        assertThat(service.getTemplateById(1L)).isSameAs(template);
        assertThatThrownBy(() -> service.getTemplateById(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void fallbackGetTemplatesReturnsEmptyList() {
        assertThat(service.fallbackGetTemplates(new RuntimeException("db down"))).isEmpty();
    }

    @Test
    void createPaymentOrderCreatesRazorpayOrderAndReturnsId() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKey", "rzp-key");
        ReflectionTestUtils.setField(service, "razorpaySecret", "rzp-secret");
        OrderClient orderClient = mock(OrderClient.class);
        Order order = new Order(new JSONObject().put("id", "order_123"));
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> mock.orders = orderClient)) {
            String result = service.createPaymentOrder(99.99);

            assertThat(result).isEqualTo("order_123");
            assertThat(mocked.constructed()).hasSize(1);
            ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
            verify(orderClient).create(captor.capture());
            assertThat(captor.getValue().getInt("amount")).isEqualTo(9999);
            assertThat(captor.getValue().getString("currency")).isEqualTo("INR");
        }
    }
}
