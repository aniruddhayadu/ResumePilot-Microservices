package com.resumepilot.export.controller;

import com.resumepilot.export.entity.ExportJob;
import com.resumepilot.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ExportResource {

	private final ExportService exportService;

	@PostMapping("/pdf/{resumeId}/{userId}")
	public ResponseEntity<ExportJob> exportPdf(@PathVariable int resumeId, @PathVariable int userId,
			@RequestBody(required = false) Map<String, String> customizations) {
		return ResponseEntity.ok(exportService.exportToPdf(resumeId, userId, customizations));
	}

	@GetMapping("/status/{jobId}")
	public ResponseEntity<ExportJob> getJobStatus(@PathVariable String jobId) {
		return ResponseEntity.ok(exportService.getJobStatus(jobId));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<ExportJob>> getByUser(@PathVariable int userId) {
		return ResponseEntity.ok(exportService.getExportsByUser(userId));
	}

	@DeleteMapping("/{jobId}")
	public ResponseEntity<String> deleteExport(@PathVariable String jobId) {
		exportService.deleteExport(jobId);
		return ResponseEntity.ok("Export job deleted successfully");
	}
}