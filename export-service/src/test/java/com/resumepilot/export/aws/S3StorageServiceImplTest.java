package com.resumepilot.export.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceImplTest {

    @Mock
    private AmazonS3 s3Client;

    private S3StorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new S3StorageServiceImpl(s3Client);
        ReflectionTestUtils.setField(service, "bucketName", "resume-bucket");
    }

    @Test
    void uploadFileUploadsBytesAndReturnsPublicUrl() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        when(s3Client.getUrl("resume-bucket", "resume.pdf")).thenReturn(new URL("https://s3.test/resume.pdf"));
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        String result = service.uploadFile("resume.pdf", new byte[] {1, 2, 3}, "application/pdf");

        assertThat(result).isEqualTo("https://s3.test/resume.pdf");
        org.mockito.Mockito.verify(s3Client).putObject(captor.capture());
        assertThat(captor.getValue().getBucketName()).isEqualTo("resume-bucket");
        assertThat(captor.getValue().getKey()).isEqualTo("resume.pdf");
        assertThat(captor.getValue().getMetadata().getContentLength()).isEqualTo(3);
        assertThat(captor.getValue().getMetadata().getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void uploadFileWrapsS3Failures() {
        when(s3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("S3 down"));

        assertThatThrownBy(() -> service.uploadFile("resume.pdf", new byte[] {1}, "application/pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 Upload Failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
