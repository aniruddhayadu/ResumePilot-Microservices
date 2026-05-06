package com.resumepilot.template;

import com.resumepilot.template.entity.Template;
import com.resumepilot.template.repository.TemplateRepository;
import com.resumepilot.template.service.TemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
}
