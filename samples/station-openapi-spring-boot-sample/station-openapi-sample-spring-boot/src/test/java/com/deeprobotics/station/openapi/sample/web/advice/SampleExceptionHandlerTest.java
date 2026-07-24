package com.deeprobotics.station.openapi.sample.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sample.safety.DangerousOperationDisabledException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiHttpException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class SampleExceptionHandlerTest {

    private final SampleExceptionHandler handler = new SampleExceptionHandler(messageSource());

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void validationUsesChineseMessageAndNeverEchoesRejectedValue() throws Exception {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        ProblemDetail detail = handler.handleValidation(validationException("camera-password"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("参数校验失败");
        assertThat(detail.getDetail()).isEqualTo("查询四足狗状态时 dogCode 不能为空");
        assertThat(detail.getProperties())
                .containsEntry("code", "SAMPLE_VALIDATION_FAILED")
                .containsEntry("field", "dogCode");
        assertThat(detail.toString()).doesNotContain("camera-password");
    }

    @Test
    void validationUsesEnglishMessage() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);

        ProblemDetail detail = handler.handleValidation(validationException("rejected-value"));

        assertThat(detail.getTitle()).isEqualTo("Validation failed");
        assertThat(detail.getDetail())
                .isEqualTo("dogCode is required when querying robot dog status.");
    }

    @Test
    void disabledDangerousOperationReturnsLocalizedForbiddenProblem() {
        LocaleContextHolder.setLocale(Locale.US);

        ProblemDetail detail = handler.handleDangerousOperation(
                new DangerousOperationDisabledException());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(detail.getTitle()).isEqualTo("Dangerous operation disabled");
        assertThat(detail.getProperties())
                .containsEntry("code", "SAMPLE_DANGEROUS_OPERATION_DISABLED");
    }

    @Test
    void sdkClientErrorReturnsBadRequest() {
        LocaleContextHolder.setLocale(Locale.US);

        ProblemDetail detail = handler.handleClient(
                new StationOpenApiClientException("invalid request"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("SDK request parameter or configuration error");
        assertThat(detail.getProperties()).containsEntry("code", "SAMPLE_SDK_CLIENT_ERROR");
    }

    @Test
    void sdkAuthErrorReturnsUnauthorizedAndPreservesContext() {
        LocaleContextHolder.setLocale(Locale.US);
        StationOpenApiAuthException exception = new StationOpenApiAuthException(
                "authentication rejected", "/remoteApi/time", "request-401", "system.getTimeText", 401);

        ProblemDetail detail = handler.handleAuth(exception);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(detail.getProperties())
                .containsEntry("requestId", "request-401")
                .containsEntry("operation", "system.getTimeText")
                .containsEntry("path", "/remoteApi/time")
                .containsEntry("code", 401)
                .containsEntry("resultUnknown", false);
    }

    @Test
    void businessErrorReturnsUnprocessableEntityAndKeepsRedactedPlatformMessage() {
        LocaleContextHolder.setLocale(Locale.US);
        StationOpenApiBusinessException exception = new StationOpenApiBusinessException(
                460, "backend rejected", "/remoteApi/taskPage", "request-460", "task.page");

        ProblemDetail detail = handler.handleBusiness(exception);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getTitle()).isEqualTo("Station business request failed");
        assertThat(detail.getDetail()).contains("backend rejected");
        assertThat(detail.getProperties()).containsEntry("code", 460);
    }

    @Test
    void timeoutReturnsGatewayTimeoutAndPreservesUnknownResult() {
        LocaleContextHolder.setLocale(Locale.US);
        StationOpenApiTimeoutException exception = new StationOpenApiTimeoutException(
                "timed out", "/remoteApi/controlDog", "request-timeout", "dog.control", true);

        ProblemDetail detail = handler.handleTimeout(exception);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(detail.getProperties())
                .containsEntry("requestId", "request-timeout")
                .containsEntry("operation", "dog.control")
                .containsEntry("path", "/remoteApi/controlDog")
                .containsEntry("code", "SAMPLE_SDK_TIMEOUT")
                .containsEntry("resultUnknown", true);
    }

    @TestFactory
    Stream<DynamicTest> upstreamSdkErrorsReturnBadGateway() {
        LocaleContextHolder.setLocale(Locale.US);
        return Stream.of(
                new StationOpenApiHttpException(
                        503, "/remoteApi/time", "request-http", "system.getTimeText", "unavailable"),
                new StationOpenApiTransportException(
                        "connection reset", "/remoteApi/time", "request-transport", "system.getTimeText", false),
                new StationOpenApiSerializationException(
                        "invalid JSON", "/remoteApi/time", "request-json", "system.getTimeText", null),
                new StationOpenApiDownloadException(
                        "download failed", "/remoteApi/result/resource", "request-download",
                        "result.downloadResource", true))
                .map(exception -> DynamicTest.dynamicTest(
                        exception.getClass().getSimpleName(),
                        () -> assertBadGateway(exception)));
    }

    private void assertBadGateway(StationOpenApiException exception) {
        ProblemDetail detail = handler.handleUpstream(exception);
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(detail.getTitle()).isEqualTo("Station request failed");
        assertThat(detail.getProperties()).containsEntry("resultUnknown", exception.resultUnknown());
    }

    private static MethodArgumentNotValidException validationException(Object rejectedValue) throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request",
                "dogCode",
                rejectedValue,
                false,
                null,
                null,
                "REMOTE_API_DOG_PROP_DOG_CODE_REQUIRED"));
        Method method = SampleExceptionHandlerTest.class.getDeclaredMethod("validationProbe", Object.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private static void validationProbe(Object request) {
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
