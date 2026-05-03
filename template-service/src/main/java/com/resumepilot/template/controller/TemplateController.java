package com.resumepilot.template.controller;

import com.resumepilot.template.entity.Template;
import com.resumepilot.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Ye jaruri hai CrossOrigin ke liye

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Template Management", description = "APIs for Resume Templates")
public class TemplateController {

	private final TemplateService templateService;

	@GetMapping
	@Operation(summary = "Get all available templates", description = "Fetches both Free and Premium templates")
	public ResponseEntity<List<Template>> getAllTemplates() {
		return ResponseEntity.ok(templateService.getAllTemplates());
	}

	@PostMapping
	@Operation(summary = "Add a new template", description = "Admin utility to add templates using @Builder pattern")
	public ResponseEntity<Template> addTemplate(@RequestBody Template template) {
		return ResponseEntity.ok(templateService.saveTemplate(template));
	}
}