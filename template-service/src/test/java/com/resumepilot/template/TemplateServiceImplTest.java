package com.resumepilot.template;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.resumepilot.template.entity.Template;
import com.resumepilot.template.repository.TemplateRepository;
import com.resumepilot.template.service.TemplateServiceImpl;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

	@Mock
	private TemplateRepository repo;

	@InjectMocks
	private TemplateServiceImpl srv;

	private Template tpl;

	@BeforeEach
	void setup() {
		tpl = new Template();
		tpl.setId(1L);
		tpl.setName("Pro ATS");
		tpl.setIsPremium(true);
		tpl.setPrice(49.0);
	}

	@Test
	void testGetAll() {
		when(repo.findAll()).thenReturn(Arrays.asList(tpl));

		List<Template> res = srv.getAllTemplates();

		assertEquals(1, res.size());
		assertEquals("Pro ATS", res.get(0).getName());
		verify(repo, times(1)).findAll();
	}

	@Test
	void testGetById() {
		when(repo.findById(1L)).thenReturn(Optional.of(tpl));

		Template res = srv.getTemplateById(1L);

		assertNotNull(res);
		assertEquals(1L, res.getId());
		verify(repo, times(1)).findById(1L);
	}
}