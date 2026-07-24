package com.deeprobotics.station.openapi.sample.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SampleRequestOptionsTest {

    @Test
    void preservesCustomerRequestId() {
        assertThat(SampleRequestOptions.from(" request-123 ").requestId())
                .isEqualTo("request-123");
    }

    @Test
    void leavesMissingRequestIdForSdkGeneration() {
        assertThat(SampleRequestOptions.from(null).requestId()).isNull();
    }
}
