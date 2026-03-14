package com.revplay.musicplatform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ServiceLoggingAspectTest {

    private static final String METHOD_SIGNATURE = "SongService.create(..)";
    private static final String RESULT_VALUE = "ok";
    private static final String FAILURE_MESSAGE = "failed";

    private final ServiceLoggingAspect serviceLoggingAspect = new ServiceLoggingAspect();

    @Test
    @DisplayName("logServiceCall returns proceeded result for successful invocation")
    void logServiceCallSuccessReturnsProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(METHOD_SIGNATURE);
        when(joinPoint.proceed()).thenReturn(RESULT_VALUE);

        Object result = serviceLoggingAspect.logServiceCall(joinPoint);

        assertThat(result).isEqualTo(RESULT_VALUE);
    }

    @Test
    @DisplayName("logServiceCall rethrows exception from target invocation")
    void logServiceCallFailureRethrowsException() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(METHOD_SIGNATURE);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException(FAILURE_MESSAGE));

        assertThatThrownBy(() -> serviceLoggingAspect.logServiceCall(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(FAILURE_MESSAGE);
    }
}
