package com.deeprobotics.station.openapi.sdk.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SDK 异常层级、上下文和敏感信息脱敏测试。
 *
 * <p>覆盖 JSON、header、key=value、Bearer、截断 JSON、cause 和完整 stack trace 等常见泄露入口。
 * 这些测试是安全闸门：新增异常包装或日志字符串时，必须保证 secretKey、完整 token、sign 和 password
 * 仍不会出现在对外输出的文本中。</p>
 */
class ExceptionRedactionTest {

    @Test
    void exceptionHierarchyExists() {
        assertThat(new StationOpenApiClientException("client error"))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiAuthException("auth error"))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiHttpException(500, "/remoteApi/taskPage", "request-1", "task.page",
                "server returned token=raw-token"))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiBusinessException(40001, "business error", "/remoteApi/taskPage", "request-1",
                "task.page"))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiTimeoutException("timeout", "/remoteApi/taskCtrl", "request-1", "task.control",
                true))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiTransportException("transport", "/remoteApi/taskCtrl", "request-1",
                "task.control", true))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiSerializationException("json error"))
                .isInstanceOf(StationOpenApiException.class);
        assertThat(new StationOpenApiDownloadException("download error", "/remoteApi/result/resource", "request-1",
                "result.downloadResource", true))
                .isInstanceOf(StationOpenApiException.class);
    }

    @Test
    void timeoutAndTransportExceptionsCanCarryResultUnknown() {
        StationOpenApiTimeoutException timeout = new StationOpenApiTimeoutException(
                "read timeout", "/remoteApi/issue", "request-1", "task.issue", true);
        StationOpenApiTransportException transport = new StationOpenApiTransportException(
                "connection reset", "/remoteApi/controlDog", "request-2", "dog.control", true);

        assertThat(timeout.resultUnknown()).isTrue();
        assertThat(transport.resultUnknown()).isTrue();
    }

    @Test
    void exceptionsCarryCommonContextWhenAvailable() {
        StationOpenApiBusinessException exception = new StationOpenApiBusinessException(
                50001, "backend rejected request", "/remoteApi/taskPage", "request-123", "task.page");

        assertThat(exception.code()).isEqualTo(50001);
        assertThat(exception.path()).isEqualTo("/remoteApi/taskPage");
        assertThat(exception.requestId()).isEqualTo("request-123");
        assertThat(exception.operation()).isEqualTo("task.page");
        assertThat(exception.resultUnknown()).isFalse();
    }

    @Test
    void exceptionMessagesRedactSensitiveFields() {
        String raw = "secretKey=raw-secret accessToken=raw-access X-Access-Token: raw-header "
                + "sign=ABCDEF012345 token=raw-token password=pwd-123";

        StationOpenApiException exception = new StationOpenApiClientException(raw);

        assertThat(exception.getMessage())
                .contains("<redacted>")
                .doesNotContain("raw-secret")
                .doesNotContain("raw-access")
                .doesNotContain("raw-header")
                .doesNotContain("ABCDEF012345")
                .doesNotContain("raw-token")
                .doesNotContain("pwd-123");
        assertThat(exception.toString())
                .doesNotContain("raw-secret")
                .doesNotContain("raw-access")
                .doesNotContain("raw-header")
                .doesNotContain("ABCDEF012345")
                .doesNotContain("raw-token")
                .doesNotContain("pwd-123");
    }

    @Test
    void redactorHandlesJsonHeaderAndQueryLikeShapes() {
        String raw = "{\"secretKey\":\"raw-secret\",\"accessToken\":\"raw-access\",\"sign\":\"raw-sign\","
                + "\"password\":\"raw-password\"}\nX-Access-Token: raw-header-token&token=raw-query-token";

        String redacted = Redactor.redact(raw);

        assertThat(redacted)
                .contains("<redacted>")
                .doesNotContain("raw-secret")
                .doesNotContain("raw-access")
                .doesNotContain("raw-sign")
                .doesNotContain("raw-password")
                .doesNotContain("raw-header-token")
                .doesNotContain("raw-query-token");
    }

    @Test
    void redactorFailsClosedWhenSensitiveJsonStringIsTruncated() {
        Map<String, String> truncatedInputs = new LinkedHashMap<>();
        truncatedInputs.put("{\"secretKey\":\"raw-secret", "raw-secret");
        truncatedInputs.put("{\"accessToken\":\"raw-access\\", "raw-access");
        truncatedInputs.put("{\"sign\":\"raw-sign\\u00", "raw-sign");
        truncatedInputs.put("{\"password\":\"raw-password", "raw-password");

        truncatedInputs.forEach((raw, secret) -> assertThat(Redactor.redact(raw))
                .as("truncated sensitive field must be redacted: %s", raw.substring(0, raw.indexOf(':')))
                .contains("<redacted>")
                .doesNotContain(secret));
    }

    @Test
    void redactorDoesNotLeakBearerHeaderValueWithSpaces() {
        String raw = "X-Access-Token: Bearer raw-token tail\r\nContent-Type: application/json";

        String redacted = Redactor.redact(raw);

        assertThat(redacted)
                .contains("X-Access-Token: <redacted>")
                .contains("Content-Type: application/json")
                .doesNotContain("Bearer")
                .doesNotContain("raw-token")
                .doesNotContain("tail");
    }

    @Test
    void redactorDoesNotLeakQuotedKeyValueWithSpaces() {
        String raw = "camera password=\"abc def ghi\" token='one two three' path=/remoteApi/cameraCtrl";

        String redacted = Redactor.redact(raw);

        assertThat(redacted)
                .contains("password=<redacted>")
                .contains("token=<redacted>")
                .contains("path=/remoteApi/cameraCtrl")
                .doesNotContain("abc")
                .doesNotContain("def")
                .doesNotContain("ghi")
                .doesNotContain("one")
                .doesNotContain("two")
                .doesNotContain("three");
    }

    @Test
    void exceptionCauseIsRedactedInMessagesAndStackTrace() {
        RuntimeException cause = new RuntimeException(
                "upstream failed X-Access-Token: Bearer raw-cause-token tail password=\"abc def\"");

        StationOpenApiException exception = new StationOpenApiSerializationException(
                "failed token=raw-message-token", cause);

        String stackTrace = stackTraceOf(exception);

        assertThat(exception.getMessage())
                .doesNotContain("raw-message-token")
                .contains("<redacted>");
        assertThat(exception.toString())
                .doesNotContain("raw-message-token")
                .doesNotContain("raw-cause-token")
                .doesNotContain("abc def");
        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause().toString())
                .contains("<redacted>")
                .doesNotContain("raw-cause-token")
                .doesNotContain("tail")
                .doesNotContain("abc")
                .doesNotContain("def");
        assertThat(stackTrace)
                .contains("<redacted>")
                .doesNotContain("raw-message-token")
                .doesNotContain("raw-cause-token")
                .doesNotContain("tail")
                .doesNotContain("abc")
                .doesNotContain("def");
    }

    private static String stackTraceOf(Throwable throwable) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        throwable.printStackTrace(new PrintStream(output, true, StandardCharsets.UTF_8));
        return output.toString(StandardCharsets.UTF_8);
    }
}
