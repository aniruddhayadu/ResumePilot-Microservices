package com.resumepilot.resume;

import com.resumepilot.resume.dto.ResumeDTO;
import com.resumepilot.resume.entity.Resume;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeModelTest {

    @Test
    void resumeConstructorAndAccessorsWork() {
        LocalDateTime createdAt = LocalDateTime.now();
        Resume resume = new Resume(1L, "user@example.com", "Backend Developer", "content", createdAt);

        assertThat(resume.getId()).isEqualTo(1L);
        assertThat(resume.getUserEmail()).isEqualTo("user@example.com");
        assertThat(resume.getTitle()).isEqualTo("Backend Developer");
        assertThat(resume.getContent()).isEqualTo("content");
        assertThat(resume.getCreatedAt()).isEqualTo(createdAt);

        LocalDateTime updatedAt = createdAt.plusDays(1);
        resume.setId(2L);
        resume.setUserEmail("new@example.com");
        resume.setTitle("Full Stack Developer");
        resume.setContent("new-content");
        resume.setCreatedAt(updatedAt);

        assertThat(resume.getId()).isEqualTo(2L);
        assertThat(resume.getUserEmail()).isEqualTo("new@example.com");
        assertThat(resume.getTitle()).isEqualTo("Full Stack Developer");
        assertThat(resume.getContent()).isEqualTo("new-content");
        assertThat(resume.getCreatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void resumeDtoAccessorsWork() {
        ResumeDTO dto = new ResumeDTO();

        dto.setTitle("Backend Developer");
        dto.setContent("content");
        dto.setTemplateId("classic");
        dto.setUserEmail("user@example.com");

        assertThat(dto.getTitle()).isEqualTo("Backend Developer");
        assertThat(dto.getContent()).isEqualTo("content");
        assertThat(dto.getTemplateId()).isEqualTo("classic");
        assertThat(dto.getUserEmail()).isEqualTo("user@example.com");
    }
}
