package com.resumepilot.export.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportJobTest {

    @Test
    void onCreateSetsRequestedAndExpiryDates() {
        ExportJob job = new ExportJob();

        job.onCreate();

        assertThat(job.getRequestedAt()).isNotNull();
        assertThat(job.getExpiresAt()).isAfter(job.getRequestedAt());
    }
}
