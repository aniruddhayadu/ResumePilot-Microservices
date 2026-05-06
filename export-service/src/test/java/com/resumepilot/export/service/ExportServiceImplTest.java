package com.resumepilot.export.service;

import com.resumepilot.export.aws.S3StorageService;
import com.resumepilot.export.dto.ResumeDto;
import com.resumepilot.export.entity.ExportJob;
import com.resumepilot.export.enums.JobStatus;
import com.resumepilot.export.repository.ExportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ExportRepository exportRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExportServiceImpl service;

    @Test
    void exportToPdfCompletesJobAndUploadsPdf() {
        ResumeDto resume = new ResumeDto();
        resume.setTitle("Java Developer");
        resume.setUserEmail("palak@example.com");
        resume.setContent("{\"fullName\":\"Palak\",\"phone\":\"7894561235\",\"summary\":\"Good dev\",\"skills\":\"Java\",\"experience\":\"Spring\",\"education\":\"BTech\"}");

        when(exportRepository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.getForObject("http://RESUME-SERVICE/resume/get/10", ResumeDto.class)).thenReturn(resume);
        when(s3StorageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf"))).thenReturn("s3://resume.pdf");

        ExportJob job = service.exportToPdf(10, 99, Map.of("template", "modern"));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getFileUrl()).isEqualTo("s3://resume.pdf");
        assertThat(job.getFileSizeKb()).isGreaterThanOrEqualTo(0);
        assertThat(job.getCustomizations()).contains("template");
        verify(s3StorageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void exportToPdfMarksJobFailedWhenResumeMissing() {
        when(exportRepository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.getForObject("http://RESUME-SERVICE/resume/get/10", ResumeDto.class)).thenReturn(null);

        ExportJob job = service.exportToPdf(10, 99, null);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void getJobStatusReturnsJobOrThrows() {
        ExportJob job = new ExportJob();
        job.setJobId("job-1");
        when(exportRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(exportRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(service.getJobStatus("job-1")).isSameAs(job);
        assertThatThrownBy(() -> service.getJobStatus("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Export Job not found");
    }

    @Test
    void listAndDeleteDelegateToRepository() {
        ExportJob job = new ExportJob();
        when(exportRepository.findByUserId(99)).thenReturn(List.of(job));

        assertThat(service.getExportsByUser(99)).containsExactly(job);

        service.deleteExport("job-1");
        verify(exportRepository).deleteById("job-1");
    }
}
