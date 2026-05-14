package com.resumepilot.export.config;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ExportConfigTest {

    @Test
    void appConfigCreatesRestTemplate() {
        RestTemplate restTemplate = new AppConfig().restTemplate();

        assertThat(restTemplate).isNotNull();
    }

    @Test
    void awsConfigCreatesS3Client() {
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKey", "access-key");
        ReflectionTestUtils.setField(config, "secretKey", "secret-key");
        ReflectionTestUtils.setField(config, "region", "us-east-1");

        AmazonS3 client = config.s3Client();

        assertThat(client).isNotNull();
    }
}
