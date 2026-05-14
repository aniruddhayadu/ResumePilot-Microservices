package com.resumepilot.export.controller;

import com.resumepilot.export.entity.ExportJob;
import com.resumepilot.export.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportResourceTest {

    @Mock
    private ExportService exportService;

    private ExportResource controller;
    private ExportJob job;

    @BeforeEach
    void setUp() {
        controller = new ExportResource(exportService);
        job = new ExportJob();
        job.setJobId("job-1");
    }

    @Test
    void exportPdfDelegatesToService() {
        Map<String, String> customizations = Map.of("template", "modern");
        when(exportService.exportToPdf(10, 99, customizations)).thenReturn(job);

        ResponseEntity<ExportJob> response = controller.exportPdf(10, 99, customizations);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(job);
    }

    @Test
    void getJobStatusDelegatesToService() {
        when(exportService.getJobStatus("job-1")).thenReturn(job);

        ResponseEntity<ExportJob> response = controller.getJobStatus("job-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(job);
    }

    @Test
    void getByUserReturnsUserExports() {
        when(exportService.getExportsByUser(99)).thenReturn(List.of(job));

        ResponseEntity<List<ExportJob>> response = controller.getByUser(99);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(job);
    }

    @Test
    void deleteExportDelegatesToService() {
        ResponseEntity<String> response = controller.deleteExport("job-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Export job deleted successfully");
        verify(exportService).deleteExport("job-1");
    }
}
