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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResumeServiceImplTest {

	@Mock
	private ResumeRepository resumeRepository;

	@InjectMocks
	private ResumeServiceImpl resumeService;

	private Resume dummyResume;

	@BeforeEach
	void setUp() {
		dummyResume = new Resume();
		// Dhyan de: Long values ke aage 'L' lagaya hai
		dummyResume.setId(1L);
		dummyResume.setUserEmail("testuser@capgemini.com");
		dummyResume.setTitle("Software Developer - Capgemini");
	}

	// resume save check
	@Test
	void testSaveResume_Success() {
		when(resumeRepository.save(any(Resume.class))).thenReturn(dummyResume);

		Resume savedResume = resumeService.saveResume(dummyResume);

		assertNotNull(savedResume);
		assertEquals("Software Developer - Capgemini", savedResume.getTitle());
		verify(resumeRepository, times(1)).save(any(Resume.class));
	}

	// TEST 2: fetching resume by email
	@Test
	void testGetResumesByEmail_Found() {
		when(resumeRepository.findByUserEmail("testuser@capgemini.com")).thenReturn(List.of(dummyResume));

		List<Resume> resultList = resumeService.getResumesByEmail("testuser@capgemini.com");

		assertNotNull(resultList);
		assertEquals(1, resultList.size());
		assertEquals("testuser@capgemini.com", resultList.get(0).getUserEmail());
	}

	// fetching resume by id
	@Test
	void testGetResumeById_Found() {

		when(resumeRepository.findById(1L)).thenReturn(Optional.of(dummyResume));

		Resume result = resumeService.getResumeById(1L);

		assertNotNull(result);
		assertEquals(1L, result.getId());
	}

	// if id not found
	@Test
	void testGetResumeById_NotFound() {
		when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			resumeService.getResumeById(999L);
		});
	}

	// delete resume
	@Test
	void testDeleteResume_Success() {
		when(resumeRepository.existsById(1L)).thenReturn(true);
		doNothing().when(resumeRepository).deleteById(1L);

		assertDoesNotThrow(() -> resumeService.deleteResume(1L));

		verify(resumeRepository, times(1)).deleteById(1L);
	}

	// wrong id check
	@Test
	void testDeleteResume_NotFound() {
		when(resumeRepository.existsById(999L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> {
			resumeService.deleteResume(999L);
		});

		verify(resumeRepository, never()).deleteById(anyLong());
	}
}