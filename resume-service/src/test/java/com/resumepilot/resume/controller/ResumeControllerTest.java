package com.resumepilot.resume.controller;

import com.resumepilot.resume.entity.Resume;
import com.resumepilot.resume.repository.ResumeRepository;
import com.resumepilot.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    private ResumeController controller;
    private Resume resume;

    @BeforeEach
    void setUp() {
        controller = new ResumeController(resumeService, resumeRepository);
        resume = new Resume();
        resume.setId(1L);
        resume.setUserEmail("user@example.com");
        resume.setTitle("Backend Developer");
        resume.setContent("{\"summary\":\"Java\"}");
    }

    @Test
    void getResumeByIdReturnsResumeWhenFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        ResponseEntity<Resume> response = controller.getResumeById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resume);
    }

    @Test
    void getResumeByIdReturnsNotFoundWhenMissing() {
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Resume> response = controller.getResumeById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void createResumeSetsUserEmailAndSaves() {
        when(resumeService.saveResume(resume)).thenReturn(resume);

        ResponseEntity<Resume> response = controller.createResume(resume, "owner@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resume);
        assertThat(resume.getUserEmail()).isEqualTo("owner@example.com");
    }

    @Test
    void getMyResumesReturnsCurrentUsersResumes() {
        when(resumeService.getResumesByEmail("user@example.com")).thenReturn(List.of(resume));

        ResponseEntity<List<Resume>> response = controller.getMyResumes("user@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resume);
    }

    @Test
    void deleteResumeReturnsSuccessMessage() {
        ResponseEntity<Map<String, String>> response = controller.deleteResume(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Resume deleted successfully");
        verify(resumeService).deleteResume(1L);
    }

    @Test
    void updateResumeUpdatesOnlyOwnerResume() {
        Resume updated = new Resume();
        updated.setTitle("Senior Backend Developer");
        updated.setContent("{\"summary\":\"Spring Boot\"}");
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        ResponseEntity<?> response = controller.updateResume(1L, updated, "user@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resume);
        assertThat(resume.getTitle()).isEqualTo("Senior Backend Developer");
        assertThat(resume.getContent()).isEqualTo("{\"summary\":\"Spring Boot\"}");
        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeService).saveResume(captor.capture());
        assertThat(captor.getValue()).isSameAs(resume);
    }

    @Test
    void updateResumeRejectsNonOwner() {
        Resume updated = new Resume();
        updated.setTitle("Senior Backend Developer");
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        ResponseEntity<?> response = controller.updateResume(1L, updated, "other@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Unauthorized edit attempt");
    }

    @Test
    void updateResumeReturnsNotFoundWhenMissing() {
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateResume(99L, new Resume(), "user@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
