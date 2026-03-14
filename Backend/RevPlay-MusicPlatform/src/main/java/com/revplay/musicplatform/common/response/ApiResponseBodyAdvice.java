package com.revplay.musicplatform.common.response;

import java.time.LocalDateTime;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.revplay")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body == null) {
            return ApiResponse.builder()
                    .success(true)
                    .message("Success")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        if (body instanceof ApiResponse<?> || body instanceof Resource || body instanceof byte[]
                || body instanceof ProblemDetail || body instanceof String) {
            return body;
        }
        return ApiResponse.builder()
                .success(true)
                .message("Success")
                .data(body)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

