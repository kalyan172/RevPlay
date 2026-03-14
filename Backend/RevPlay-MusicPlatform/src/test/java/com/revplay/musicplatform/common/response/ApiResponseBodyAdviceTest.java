package com.revplay.musicplatform.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

@Tag("unit")
class ApiResponseBodyAdviceTest {

    private static final String SUCCESS_MESSAGE = "Success";
    private static final String TEXT_BODY = "text-response";
    private static final String PROBLEM_DETAIL = "problem";
    private static final byte[] BYTES = new byte[]{1, 2, 3};

    private final ApiResponseBodyAdvice advice = new ApiResponseBodyAdvice();
    private final ServerHttpRequest request = mock(ServerHttpRequest.class);
    private final ServerHttpResponse response = mock(ServerHttpResponse.class);

    @Test
    @DisplayName("supports returns true for any return type")
    void supportsAlwaysReturnsTrue() {
        boolean supported = advice.supports(null, StringHttpMessageConverter.class);

        assertThat(supported).isTrue();
    }

    @Test
    @DisplayName("beforeBodyWrite wraps null body into success ApiResponse")
    void beforeBodyWriteWrapsNullBody() {
        Object result = advice.beforeBodyWrite(
                null,
                null,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isInstanceOf(ApiResponse.class);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result;
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).isEqualTo(SUCCESS_MESSAGE);
        assertThat(apiResponse.getData()).isNull();
        assertThat(apiResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("beforeBodyWrite returns ApiResponse as is")
    void beforeBodyWriteReturnsApiResponseAsIs() {
        ApiResponse<String> body = ApiResponse.success(SUCCESS_MESSAGE, "value");

        Object result = advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    @DisplayName("beforeBodyWrite returns resource as is")
    void beforeBodyWriteReturnsResourceAsIs() {
        ByteArrayResource resource = new ByteArrayResource(BYTES);

        Object result = advice.beforeBodyWrite(
                resource,
                null,
                MediaType.APPLICATION_OCTET_STREAM,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isSameAs(resource);
    }

    @Test
    @DisplayName("beforeBodyWrite returns bytes as is")
    void beforeBodyWriteReturnsBytesAsIs() {
        Object result = advice.beforeBodyWrite(
                BYTES,
                null,
                MediaType.APPLICATION_OCTET_STREAM,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isSameAs(BYTES);
    }

    @Test
    @DisplayName("beforeBodyWrite returns problem detail as is")
    void beforeBodyWriteReturnsProblemDetailAsIs() {
        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setDetail(PROBLEM_DETAIL);

        Object result = advice.beforeBodyWrite(
                problemDetail,
                null,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isSameAs(problemDetail);
    }

    @Test
    @DisplayName("beforeBodyWrite returns string as is")
    void beforeBodyWriteReturnsStringAsIs() {
        Object result = advice.beforeBodyWrite(
                TEXT_BODY,
                null,
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isEqualTo(TEXT_BODY);
    }

    @Test
    @DisplayName("beforeBodyWrite wraps non special body in success ApiResponse")
    void beforeBodyWriteWrapsNormalBody() {
        Map<String, Object> body = Map.of("k", "v");

        Object result = advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                request,
                response
        );

        assertThat(result).isInstanceOf(ApiResponse.class);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result;
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getMessage()).isEqualTo(SUCCESS_MESSAGE);
        assertThat(apiResponse.getData()).isEqualTo(body);
        assertThat(apiResponse.getTimestamp()).isNotNull();
    }
}
