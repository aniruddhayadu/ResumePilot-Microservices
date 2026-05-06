package com.resumepilot.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayApplicationTests {

    @Test
    void applicationClassIsAvailable() {
        assertThat(ApiGatewayApplication.class).isNotNull();
    }
}
