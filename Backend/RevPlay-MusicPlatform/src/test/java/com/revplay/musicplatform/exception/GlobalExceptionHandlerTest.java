package com.revplay.musicplatform.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.revplay.musicplatform.catalog.exception.DiscoveryNotFoundException;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.user.exception.AuthConflictException;
import com.revplay.musicplatform.user.exception.AuthForbiddenException;
import com.revplay.musicplatform.user.exception.AuthNotFoundException;
import com.revplay.musicplatform.user.exception.AuthUnauthorizedException;
import com.revplay.musicplatform.user.exception.AuthValidationException;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Tag("unit")
class GlobalExceptionHandlerTest {

        private static final String MESSAGE = "problem";
        private static final String VALIDATION_FAILED = "Validation failed";
        private static final String TEST_FIELD = "email";
        private static final String TEST_REASON = "invalid";
        private static final String PARAM_NAME = "songId";
        private static final String UNEXPECTED_ERROR = "Unexpected error";
        private static final String NO_STATIC_RESOURCE = "No static resource";

        private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

        @Test
        @DisplayName("not found family handlers return 404")
        void notFoundHandlersReturn404() {
                assertStatusAndMessage(handler.handleNotFound(new ResourceNotFoundException(MESSAGE)),
                                HttpStatus.NOT_FOUND,
                                MESSAGE);

                ResponseEntity<ApiResponse<Void>> noResourceResponse = handler.handleNoResource(
                                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/x"));
                assertThat(noResourceResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(noResourceResponse.getBody()).isNotNull();
                assertThat(noResourceResponse.getBody().getMessage()).contains(NO_STATIC_RESOURCE);

                assertStatusAndMessage(handler.handleDomainNotFound(new PlaybackNotFoundException(MESSAGE)),
                                HttpStatus.NOT_FOUND, MESSAGE);
                assertStatusAndMessage(handler.handleDomainNotFound(new DiscoveryNotFoundException(MESSAGE)),
                                HttpStatus.NOT_FOUND, MESSAGE);
                assertStatusAndMessage(handler.handleDomainNotFound(new AuthNotFoundException(MESSAGE)),
                                HttpStatus.NOT_FOUND,
                                MESSAGE);
        }

        @Test
        @DisplayName("unauthorized handlers return 401")
        void unauthorizedHandlersReturn401() {
                assertStatusAndMessage(handler.handleUnauthorized(new UnauthorizedException(MESSAGE)),
                                HttpStatus.UNAUTHORIZED,
                                MESSAGE);
                assertStatusAndMessage(handler.handleAuthUnauthorized(new AuthUnauthorizedException(MESSAGE)),
                                HttpStatus.UNAUTHORIZED, MESSAGE);
        }

        @Test
        @DisplayName("bad request handler returns 400 for BadRequestException")
        void handleBadRequestReturns400() {
                assertStatusAndMessage(handler.handleBadRequest(new BadRequestException(MESSAGE)),
                                HttpStatus.BAD_REQUEST,
                                MESSAGE);
        }

        @ParameterizedTest
        @MethodSource("requestValidationExceptions")
        @DisplayName("request validation handlers return 400")
        void requestValidationHandlersReturn400(Exception ex) {
                assertStatusAndMessage(handler.handleRequestValidation(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        private static Stream<Arguments> requestValidationExceptions() {
                return Stream.of(
                                Arguments.of(new IllegalArgumentException(MESSAGE)),
                                Arguments.of(new MissingServletRequestPartException("part")),
                                Arguments.of(new MissingServletRequestParameterException("param", "type")),
                                Arguments.of(new HttpMediaTypeNotSupportedException(MESSAGE)),
                                Arguments.of(new PlaybackValidationException(MESSAGE)),
                                Arguments.of(new DiscoveryValidationException(MESSAGE)),
                                Arguments.of(new AuthValidationException(MESSAGE)));
        }

        @Test
        @DisplayName("method argument type mismatch returns 400 with detailed message")
        void handleMethodArgumentTypeMismatchReturns400() throws Exception {
                Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("typeMismatchSample", Integer.class);
                MethodParameter methodParameter = new MethodParameter(method, 0);
                MethodArgumentTypeMismatchException mismatch = new MethodArgumentTypeMismatchException("abc",
                                Integer.class,
                                PARAM_NAME, methodParameter, null);
                ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentTypeMismatch(mismatch);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getMessage())
                                .contains("Invalid value 'abc' for 'songId'. Expected Integer.");
        }

        @Test
        @DisplayName("forbidden and conflict handlers map to expected status")
        void forbiddenAndConflictHandlersMapExpectedStatus() {
                assertStatusAndMessage(handler.handleForbidden(new AccessDeniedException(MESSAGE)),
                                HttpStatus.FORBIDDEN,
                                MESSAGE);
                assertStatusAndMessage(handler.handleForbidden(new AuthForbiddenException(MESSAGE)),
                                HttpStatus.FORBIDDEN,
                                MESSAGE);

                assertStatusAndMessage(handler.handleDuplicate(new DuplicateResourceException(MESSAGE)),
                                HttpStatus.CONFLICT,
                                MESSAGE);
                assertStatusAndMessage(handler.handleDuplicate(new AuthConflictException(MESSAGE)), HttpStatus.CONFLICT,
                                MESSAGE);
                assertStatusAndMessage(handler.handleDuplicate(new DataIntegrityViolationException(MESSAGE)),
                                HttpStatus.CONFLICT, MESSAGE);
                assertStatusAndMessage(handler.handleConflict(new ConflictException(MESSAGE)), HttpStatus.CONFLICT,
                                MESSAGE);
        }

        @Test
        @DisplayName("validation handler maps method argument not valid and bind exceptions")
        void validationHandlerMapsValidationErrors() {
                BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "target");
                result.addError(new FieldError("target", TEST_FIELD, TEST_REASON));
                MethodArgumentNotValidException manv = new MethodArgumentNotValidException(mock(MethodParameter.class),
                                result);

                ResponseEntity<ApiResponse<Void>> manvResponse = handler.handleValidation(manv);

                assertThat(manvResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(manvResponse.getBody()).isNotNull();
                assertThat(manvResponse.getBody().getMessage()).isEqualTo(VALIDATION_FAILED);
                assertThat(manvResponse.getBody().getErrors()).hasSize(1);
                assertThat(manvResponse.getBody().getErrors().get(0).getField()).isEqualTo(TEST_FIELD);
                assertThat(manvResponse.getBody().getErrors().get(0).getReason()).isEqualTo(TEST_REASON);

                BindException bindException = new BindException(new Object(), "bindTarget");
                bindException.addError(new FieldError("bindTarget", TEST_FIELD, TEST_REASON));
                ResponseEntity<ApiResponse<Void>> bindResponse = handler.handleValidation(bindException);

                assertThat(bindResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(bindResponse.getBody()).isNotNull();
                assertThat(bindResponse.getBody().getErrors()).hasSize(1);
        }

        @Test
        @DisplayName("generic handler returns 500 with unexpected error message")
        void genericHandlerReturns500() {
                ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(new RuntimeException(MESSAGE));

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getMessage()).isEqualTo(UNEXPECTED_ERROR);
                assertThat(response.getBody().isSuccess()).isFalse();
        }

        private void assertStatusAndMessage(ResponseEntity<ApiResponse<Void>> response, HttpStatus expectedStatus,
                        String expectedMessage) {
                assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
                assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @SuppressWarnings("unused")
        private static void typeMismatchSample(Integer value) {
        }
}
