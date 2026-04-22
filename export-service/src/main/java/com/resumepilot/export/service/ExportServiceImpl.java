package com.resumepilot.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.resumepilot.export.aws.S3StorageService;
import com.resumepilot.export.dto.ResumeDto;
import com.resumepilot.export.entity.ExportJob;
import com.resumepilot.export.enums.JobStatus;
import com.resumepilot.export.repository.ExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

	private final ExportRepository exportRepository;
	private final S3StorageService s3StorageService;
	private final RestTemplate restTemplate;

	@Override
	public ExportJob exportToPdf(int resumeId, int userId, Map<String, String> customizations) {
		log.info("Starting PDF generation for Resume ID: {} | User ID: {}", resumeId, userId);

		ExportJob job = new ExportJob();
		job.setJobId(UUID.randomUUID().toString());
		job.setResumeId(resumeId);
		job.setUserId(userId);
		job.setFormat("PDF");
		job.setStatus(JobStatus.PROCESSING);

		if (customizations != null) {
			job.setCustomizations(customizations.toString());
		}

		job = exportRepository.save(job);

		try {
			// fetching data from resume service
			log.info("Fetching resume data from RESUME-SERVICE for ID: {}", resumeId);
			String resumeUrl = "http://RESUME-SERVICE/resume/get/" + resumeId;
			ResumeDto resumeData = restTemplate.getForObject(resumeUrl, ResumeDto.class);

			if (resumeData == null) {
				throw new RuntimeException("Resume data not found for ID: " + resumeId);
			}

			// extracting json string
			ObjectMapper mapper = new ObjectMapper();
			JsonNode contentNode = mapper.readTree(resumeData.getContent());

			String defaultName = resumeData.getUserEmail() != null ? resumeData.getUserEmail().split("@")[0]
					: "CANDIDATE NAME";
			String fullName = contentNode.has("fullName") && !contentNode.get("fullName").asText().isEmpty()
					? contentNode.get("fullName").asText()
					: defaultName;

			String summary = contentNode.has("summary") ? contentNode.get("summary").asText() : "Not provided";
			String skills = contentNode.has("skills") ? contentNode.get("skills").asText() : "Not provided";
			String experience = contentNode.has("experience") ? contentNode.get("experience").asText() : "Not provided";
			String education = contentNode.has("education") ? contentNode.get("education").asText() : "Not provided";

			// iText 7 pdf generation engine
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfWriter writer = new PdfWriter(baos);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf);

			document.add(new Paragraph(fullName.toUpperCase()).setBold().setFontSize(22)
					.setTextAlignment(TextAlignment.CENTER));

			// Job Title
			String titleStr = resumeData.getTitle() != null ? resumeData.getTitle() : "";
			document.add(
					new Paragraph(titleStr).setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

			// Email Id
			String emailStr = resumeData.getUserEmail() != null ? resumeData.getUserEmail() : "";
			document.add(
					new Paragraph(emailStr).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

			// PDF Sections
			document.add(new Paragraph("PROFESSIONAL SUMMARY").setBold().setFontSize(14));
			document.add(new Paragraph(summary).setMarginBottom(10));

			document.add(new Paragraph("TECHNICAL SKILLS").setBold().setFontSize(14));
			document.add(new Paragraph(skills).setMarginBottom(10));

			document.add(new Paragraph("EXPERIENCE").setBold().setFontSize(14));
			document.add(new Paragraph(experience).setMarginBottom(10));

			document.add(new Paragraph("EDUCATION").setBold().setFontSize(14));
			document.add(new Paragraph(education).setMarginBottom(10));

			document.close();

			byte[] pdfBytes = baos.toByteArray();
			log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);

			// Upload to S3
			String fileName = "resume-" + resumeId + "-" + job.getJobId() + ".pdf";
			String s3Url = s3StorageService.uploadFile(fileName, pdfBytes, "application/pdf");

			// 5. Mark job complete
			job.setStatus(JobStatus.COMPLETED);
			job.setFileUrl(s3Url);
			job.setFileSizeKb(pdfBytes.length / 1024);
			job.setCompletedAt(LocalDateTime.now());
			exportRepository.save(job);

			log.info("Job {} completed. File available at {}", job.getJobId(), s3Url);

		} catch (Exception e) {
			log.error("Failed to generate PDF for Job {}: {}", job.getJobId(), e.getMessage(), e);
			job.setStatus(JobStatus.FAILED);
			exportRepository.save(job);
		}

		return job;
	}

	@Override
	public ExportJob getJobStatus(String jobId) {
		return exportRepository.findById(jobId)
				.orElseThrow(() -> new RuntimeException("Export Job not found with ID: " + jobId));
	}

	@Override
	public List<ExportJob> getExportsByUser(int userId) {
		return exportRepository.findByUserId(userId);
	}

	@Override
	public void deleteExport(String jobId) {
		exportRepository.deleteById(jobId);
	}
}