package com.resumepilot.export.service;

import com.resumepilot.export.entity.ExportJob;
import java.util.List;
import java.util.Map;

public interface ExportService {
	ExportJob exportToPdf(int resumeId, int userId, Map<String, String> customizations);

	ExportJob getJobStatus(String jobId);

	List<ExportJob> getExportsByUser(int userId);

	void deleteExport(String jobId);
}