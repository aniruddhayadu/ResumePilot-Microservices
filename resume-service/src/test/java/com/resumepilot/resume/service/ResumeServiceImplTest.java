package com.resumepilot.resume.service;

import com.resumepilot.resume.entity.Resume;
import com.resumepilot.resume.exception.ResourceNotFoundException;
import com.resumepilot.resume.repository.ResumeRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private Resume resume;

    @BeforeEach
    void setUp() {
        resume = new Resume();
        resume.setId(1L);
        resume.setUserEmail("testuser@capgemini.com");
        resume.setTitle("Software Developer");
        resume.setContent("{\"summary\":\"Java dev\"}");
    }

    @Test
    void saveResumePersistsResume() {
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        Resume savedResume = resumeService.saveResume(resume);

        assertThat(savedResume).isSameAs(resume);
        verify(resumeRepository).save(resume);
    }

    @Test
    void getResumesByEmailReturnsMatches() {
        when(resumeRepository.findByUserEmail("testuser@capgemini.com")).thenReturn(List.of(resume));

        List<Resume> result = resumeService.getResumesByEmail("testuser@capgemini.com");

        assertThat(result).containsExactly(resume);
    }

    @Test
    void getResumeByIdReturnsResumeOrThrows() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(resumeService.getResumeById(1L)).isSameAs(resume);
        assertThatThrownBy(() -> resumeService.getResumeById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume not found");
    }

    @Test
    void deleteResumeDeletesOnlyWhenFound() {
        when(resumeRepository.existsById(1L)).thenReturn(true);

        resumeService.deleteResume(1L);

        verify(resumeRepository).deleteById(1L);
    }

    @Test
    void deleteResumeThrowsWhenMissing() {
        when(resumeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> resumeService.deleteResume(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cannot delete");
        verify(resumeRepository, never()).deleteById(999L);
    }
}
