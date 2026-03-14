package com.revplay.musicplatform.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ApiResponseTest {

    private static final String SUCCESS_MESSAGE = "ok";
    private static final String ERROR_MESSAGE = "error";
    private static final String DATA_VALUE = "payload";

    @Test
    @DisplayName("success with data sets success message data and timestamp")
    void successWithDataBuildsExpectedResponse() {
        ApiResponse<String> response = ApiResponse.success(SUCCESS_MESSAGE, DATA_VALUE);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(SUCCESS_MESSAGE);
        assertThat(response.getData()).isEqualTo(DATA_VALUE);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    @DisplayName("success without data sets success true and null data")
    void successWithoutDataBuildsExpectedResponse() {
        ApiResponse<Void> response = ApiResponse.success(SUCCESS_MESSAGE);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(SUCCESS_MESSAGE);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("error sets success false and timestamp")
    void errorBuildsExpectedResponse() {
        ApiResponse<Void> response = ApiResponse.error(ERROR_MESSAGE);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(response.getTimestamp()).isNotNull();
    }
}
