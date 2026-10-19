package com.deeprobotics.station.openapi.sdk.exception;

/**
 * SDK 异常体系基类。
 *
 * <p>所有对客户抛出的异常都应携带尽可能多的排障上下文：requestId、path、operation、code
 * 和 resultUnknown。上下文会先脱敏再进入异常 message，避免客户把日志发给支持团队时泄露凭证。</p>
 */
public class StationOpenApiException extends RuntimeException {

    /**
     * 请求链路 ID，已脱敏。用于客户和平台支持人员串联日志。
     */
    private final String requestId;
    /**
     * 失败接口 path，已脱敏。
     */
    private final String path;
    /**
     * SDK 操作名，已脱敏。
     */
    private final String operation;
    /**
     * HTTP status 或平台业务 code，本地异常可能为空。
     */
    private final Integer code;
    /**
     * 执行结果是否未确认，或下载输出是否可能不完整。
     */
    private final boolean resultUnknown;

    /**
     * 创建仅包含脱敏消息的 SDK 异常。
     *
     * @param message 原始异常说明；进入异常对象前会统一脱敏
     */
    public StationOpenApiException(String message) {
        this(message, null, null, null, null, false, null);
    }

    /**
     * 创建包含脱敏消息和脱敏原因链的 SDK 异常。
     *
     * @param message 原始异常说明；进入异常对象前会统一脱敏
     * @param cause 原始原因；类型、消息、堆栈和 suppressed 异常会复制为安全原因链
     */
    public StationOpenApiException(String message, Throwable cause) {
        this(message, null, null, null, null, false, cause);
    }

    /**
     * 创建携带请求上下文但不携带原因链的 SDK 异常。
     *
     * @param message 原始异常说明
     * @param path 远端 HTTP path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param code HTTP status 或平台业务 code
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     */
    protected StationOpenApiException(String message, String path, String requestId, String operation, Integer code,
            boolean resultUnknown) {
        this(message, path, requestId, operation, code, resultUnknown, null);
    }

    /**
     * 创建携带完整请求上下文和脱敏原因链的 SDK 异常。
     *
     * @param message 原始异常说明
     * @param path 远端 HTTP path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param code HTTP status 或平台业务 code
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     * @param cause 原始原因；进入异常对象前会统一脱敏
     */
    protected StationOpenApiException(String message, String path, String requestId, String operation, Integer code,
            boolean resultUnknown, Throwable cause) {
        // message 和 cause 都必须重新包装，不能信任底层 HTTP/JSON 库的异常内容已经脱敏。
        super(formatMessage(message, path, requestId, operation, code, resultUnknown), sanitizeCause(cause));
        this.requestId = Redactor.redact(requestId);
        this.path = Redactor.redact(path);
        this.operation = Redactor.redact(operation);
        this.code = code;
        this.resultUnknown = resultUnknown;
    }

    /**
     * 调用方请求 ID。可由调用方传入，也可由 SDK 生成，用于串联 SDK 日志、平台日志和异常。
     *
     * @return 已脱敏的请求链路 ID，未提供时为 {@code null}
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 远端 HTTP path，例如 {@code /remoteApi/taskPage}。
     *
     * @return 已脱敏的远端 HTTP path，未知时为 {@code null}
     */
    public String path() {
        return path;
    }

    /**
     * SDK 业务操作标识，例如 {@code task.issue}，用于客户排查“哪个 SDK 方法失败”。
     *
     * @return 已脱敏的 SDK 操作标识，未知时为 {@code null}
     */
    public String operation() {
        return operation;
    }

    /**
     * HTTP status 或平台响应 {@code code}。本地配置/序列化错误可能为空。
     *
     * @return HTTP status、平台业务 code，或本地异常下的 {@code null}
     */
    public Integer code() {
        return code;
    }

    /**
     * 本次调用是否存在无法确认的执行结果或不完整输出。
     *
     * <p>执行器处理写操作、任务控制和设备控制的超时/连接中断时会设为 {@code true}。客户应先查询状态
     * 或等待 MQ 事件，不应直接重试。</p>
     *
     * <p>下载的连接、超时或输出失败也会设为 {@code true}，表示可能已交付部分字节。
     * 调用方应丢弃不完整输出后重新下载，不应向同一输出追加重试结果。</p>
     *
     * @return 结果无法确认时为 {@code true}
     */
    public boolean resultUnknown() {
        return resultUnknown;
    }

    private static String formatMessage(String message, String path, String requestId, String operation, Integer code,
            boolean resultUnknown) {
        StringBuilder builder = new StringBuilder();
        String redactedMessage = Redactor.redact(message);
        if (redactedMessage != null && !redactedMessage.isBlank()) {
            builder.append(redactedMessage);
        } else {
            builder.append("Station OpenAPI request failed");
        }

        StringBuilder context = new StringBuilder();
        appendContext(context, "requestId", requestId);
        appendContext(context, "path", path);
        appendContext(context, "operation", operation);
        if (code != null) {
            appendContext(context, "code", String.valueOf(code));
        }
        if (resultUnknown) {
            appendContext(context, "resultUnknown", "true");
        }
        if (context.length() > 0) {
            builder.append(" [").append(context).append(']');
        }
        return builder.toString();
    }

    private static void appendContext(StringBuilder builder, String name, String value) {
        String redactedValue = Redactor.redact(value);
        if (redactedValue == null || redactedValue.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(name).append('=').append(redactedValue);
    }

    private static Throwable sanitizeCause(Throwable cause) {
        if (cause == null) {
            return null;
        }
        // 不能直接保留 cause message；例如 HTTP 库可能把 header 或 body 片段放进异常。
        Throwable sanitized = new RedactedCause(cause);
        sanitized.setStackTrace(cause.getStackTrace());
        for (Throwable suppressed : cause.getSuppressed()) {
            sanitized.addSuppressed(sanitizeCause(suppressed));
        }
        Throwable nestedCause = sanitizeCause(cause.getCause());
        if (nestedCause != null) {
            sanitized.initCause(nestedCause);
        }
        return sanitized;
    }

    private static final class RedactedCause extends RuntimeException {

        private RedactedCause(Throwable cause) {
            super(formatCauseMessage(cause));
        }

        private static String formatCauseMessage(Throwable cause) {
            String causeType = cause.getClass().getName();
            String causeMessage = Redactor.redact(cause.getMessage());
            if (causeMessage == null || causeMessage.isBlank()) {
                return causeType;
            }
            return causeType + ": " + causeMessage;
        }
    }
}
