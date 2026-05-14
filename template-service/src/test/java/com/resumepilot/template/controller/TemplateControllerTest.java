package com.resumepilot.template.controller;

import com.resumepilot.template.entity.Template;
import com.resumepilot.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    private TemplateController controller;
    private Template template;

    @BeforeEach
    void setUp() {
        controller = new TemplateController(templateService);
        template = Template.builder().id(1L).name("Modern").isPremium(false).price(0).build();
    }

    @Test
    void getAllTemplatesReturnsServiceResults() {
        when(templateService.getAllTemplates()).thenReturn(List.of(template));

        ResponseEntity<List<Template>> response = controller.getAllTemplates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(template);
    }

    @Test
    void addTemplateSavesTemplate() {
        when(templateService.saveTemplate(template)).thenReturn(template);

        ResponseEntity<Template> response = controller.addTemplate(template);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(template);
        verify(templateService).saveTemplate(template);
    }
}
