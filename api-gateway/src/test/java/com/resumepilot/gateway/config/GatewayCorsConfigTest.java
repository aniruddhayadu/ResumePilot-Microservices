package com.resumepilot.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorsConfigTest {

    @Test
    void corsWebFilterBeanIsCreated() {
        CorsWebFilter filter = new GatewayCorsConfig().corsWebFilter();

        assertThat(filter).isNotNull();
    }
}
