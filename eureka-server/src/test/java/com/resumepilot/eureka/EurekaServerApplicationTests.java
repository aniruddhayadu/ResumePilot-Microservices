package com.resumepilot.eureka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EurekaServerApplicationTests {

    @Test
    void applicationClassIsAvailable() {
        assertThat(EurekaServerApplication.class).isNotNull();
    }
}
