package com.deeprobotics.station.openapi.sample.web.advice;

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
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把 Web Validation、本地安全门禁和 SDK 异常统一转换为双语 {@link ProblemDetail}。
 *
 * <p>响应只保留定位所需的结构化字段，不拼接凭证、原始 HTTP body 或其他敏感上下文。</p>
 *
 * <p>对于 {@code StreamingResponseBody}，如果响应已经提交后才发生异常，Spring 不保证还能将异常
 * 转换为 {@link ProblemDetail}；调用方还应校验下载是否完整。</p>
 */
@RestControllerAdvice
public class SampleExceptionHandler {

    private static final String VALIDATION_FAILED = "SAMPLE_VALIDATION_FAILED";
    private static final String DANGEROUS_OPERATION_TITLE = "SAMPLE_DANGEROUS_OPERATION_TITLE";
    private static final String DANGEROUS_OPERATION_DISABLED = "SAMPLE_DANGEROUS_OPERATION_DISABLED";
    private static final String SDK_CLIENT_ERROR = "SAMPLE_SDK_CLIENT_ERROR";
    private static final String SDK_AUTH_ERROR = "SAMPLE_SDK_AUTH_ERROR";
    private static final String SDK_BUSINESS_ERROR = "SAMPLE_SDK_BUSINESS_ERROR";
    private static final String SDK_TIMEOUT = "SAMPLE_SDK_TIMEOUT";
    private static final String SDK_UPSTREAM_ERROR = "SAMPLE_SDK_UPSTREAM_ERROR";

    private final MessageSource messageSource;

    public SampleExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 将 JSON 格式或字段类型错误统一转换为本地化 400，避免回显解析异常中的请求值。
     *
     * @param exception HTTP 消息解析异常；不把其内容写入响应或日志
     * @return 带稳定校验错误码的 ProblemDetail
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        String title = message(VALIDATION_FAILED, LocaleContextHolder.getLocale());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, title);
        detail.setTitle(title);
        detail.setProperty("code", VALIDATION_FAILED);
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Locale locale = LocaleContextHolder.getLocale();
        String title = message(VALIDATION_FAILED, locale);
        // Sample 只返回首个字段错误，保持响应简单，并避免一次回显过多请求细节。
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String detailMessage = fieldError == null
                ? title
                : message(fieldError.getDefaultMessage(), fieldError.getArguments(), locale);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detailMessage);
        detail.setTitle(title);
        detail.setProperty("code", VALIDATION_FAILED);
        if (fieldError != null) {
            detail.setProperty("field", fieldError.getField());
        }
        return detail;
    }

    @ExceptionHandler(DangerousOperationDisabledException.class)
    public ProblemDetail handleDangerousOperation(DangerousOperationDisabledException exception) {
        Locale locale = LocaleContextHolder.getLocale();
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, message(DANGEROUS_OPERATION_DISABLED, locale));
        detail.setTitle(message(DANGEROUS_OPERATION_TITLE, locale));
        detail.setProperty("code", DANGEROUS_OPERATION_DISABLED);
        return detail;
    }

    @ExceptionHandler(StationOpenApiClientException.class)
    public ProblemDetail handleClient(StationOpenApiClientException exception) {
        return sdkProblem(HttpStatus.BAD_REQUEST, SDK_CLIENT_ERROR, exception, false);
    }

    @ExceptionHandler(StationOpenApiAuthException.class)
    public ProblemDetail handleAuth(StationOpenApiAuthException exception) {
        return sdkProblem(HttpStatus.UNAUTHORIZED, SDK_AUTH_ERROR, exception, false);
    }

    @ExceptionHandler(StationOpenApiBusinessException.class)
    public ProblemDetail handleBusiness(StationOpenApiBusinessException exception) {
        return sdkProblem(HttpStatus.UNPROCESSABLE_ENTITY, SDK_BUSINESS_ERROR, exception, true);
    }

    @ExceptionHandler(StationOpenApiTimeoutException.class)
    public ProblemDetail handleTimeout(StationOpenApiTimeoutException exception) {
        return sdkProblem(HttpStatus.GATEWAY_TIMEOUT, SDK_TIMEOUT, exception, false);
    }

    @ExceptionHandler({
            StationOpenApiHttpException.class,
            StationOpenApiTransportException.class,
            StationOpenApiSerializationException.class,
            StationOpenApiDownloadException.class
    })
    public ProblemDetail handleUpstream(StationOpenApiException exception) {
        return sdkProblem(HttpStatus.BAD_GATEWAY, SDK_UPSTREAM_ERROR, exception, false);
    }

    private ProblemDetail sdkProblem(
            HttpStatus status,
            String messageKey,
            StationOpenApiException exception,
            boolean useExceptionMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        String title = message(messageKey, locale);
        String detailMessage = useExceptionMessage ? exception.getMessage() : title;
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        detail.setTitle(title);
        detail.setProperty("requestId", exception.requestId());
        detail.setProperty("operation", exception.operation());
        detail.setProperty("path", exception.path());
        detail.setProperty("code", exception.code() == null ? messageKey : exception.code());
        // resultUnknown 对写入/控制操作尤为重要，客户应先查状态或等待 MQ，再决定是否重试。
        detail.setProperty("resultUnknown", exception.resultUnknown());
        return detail;
    }

    private String message(String key, Locale locale) {
        return message(key, null, locale);
    }

    private String message(String key, Object[] arguments, Locale locale) {
        if (key == null || key.isBlank()) {
            return messageSource.getMessage(VALIDATION_FAILED, null, VALIDATION_FAILED, locale);
        }
        return messageSource.getMessage(key, arguments, key, locale);
    }
}
