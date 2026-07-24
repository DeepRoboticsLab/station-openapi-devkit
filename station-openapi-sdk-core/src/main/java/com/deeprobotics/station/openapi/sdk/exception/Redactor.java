package com.deeprobotics.station.openapi.sdk.exception;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SDK 统一脱敏工具。
 *
 * <p>任何可能进入日志、异常消息、断言消息或 {@code toString()} 的字符串都应经过这里。
 * 目标不是完整 JSON 解析，而是覆盖客户排障中常见的 JSON 字段、header、key=value 和 Bearer 形态。</p>
 */
public final class Redactor {

    /**
     * 所有敏感值统一替换成的占位文本。
     */
    public static final String REDACTED = "<redacted>";

    /**
     * 这些 key 在任何输出中都不能出现明文值。
     */
    private static final String SENSITIVE_KEYS =
            "secretKey|accessToken|X-Access-Token|Authorization|sign|token|password";
    /**
     * JSON 字段名使用小写集合判断，避免把正则表达式本身当成业务数据解析。
     */
    private static final Set<String> SENSITIVE_JSON_KEYS = Set.of(
            "secretkey", "accesstoken", "x-access-token", "authorization", "sign", "token", "password");
    /**
     * 匹配带引号的 key=value 或 key:value 片段。
     */
    private static final Pattern QUOTED_KEY_VALUE_FIELD = Pattern.compile(
            "(?i)(\\b(?:" + SENSITIVE_KEYS + ")\\b\\s*[=:]\\s*)(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')");
    /**
     * 匹配 header 风格的敏感行。
     */
    private static final Pattern HEADER_FIELD = Pattern.compile(
            "(?i)(\\b(?:" + SENSITIVE_KEYS + ")\\b\\s*:\\s*)([^\\r\\n]*)");
    /**
     * 匹配 Bearer 令牌形态，避免只脱敏 Bearer 后面的第一段。
     */
    private static final Pattern BEARER_KEY_VALUE_FIELD = Pattern.compile(
            "(?i)(\\b(?:" + SENSITIVE_KEYS + ")\\b\\s*=\\s*)Bearer\\s+[^\\r\\n&;,}\\]]+");
    /**
     * 匹配普通 key=value 形态，覆盖异常 message、Map.toString 和调试输出。
     */
    private static final Pattern KEY_VALUE_FIELD = Pattern.compile(
            "(?i)(\\b(?:" + SENSITIVE_KEYS + ")\\b\\s*=\\s*)([^\\s&;,}\\]]+)");

    private Redactor() {
    }

    /**
     * 对字符串进行尽力而为的脱敏。
     *
     * <p>该方法必须容忍非 JSON、半截响应体和异常 message，因为 HTTP 非 2xx 或下载失败时
     * executor 只会截取有限片段用于异常上下文。</p>
     *
     * @param value 可能包含敏感字段的任意字符串，允许为 {@code null}
     * @return 已替换已知敏感值的字符串
     */
    public static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        /*
         * JSON 字符串必须先处理。异常响应会先截成有限片段，截断点可能落在敏感值或转义字符中；
         * 要求闭合引号的正则会在这种输入上失配并泄露完整 token。这里用小型扫描器识别字段边界，
         * 找不到值的闭合引号时直接把输入末尾全部视为敏感值，按 fail-closed 原则处理。
         */
        String redacted = redactJsonStringFields(value);
        redacted = QUOTED_KEY_VALUE_FIELD.matcher(redacted).replaceAll("$1" + REDACTED);
        redacted = HEADER_FIELD.matcher(redacted).replaceAll("$1" + REDACTED);
        redacted = BEARER_KEY_VALUE_FIELD.matcher(redacted).replaceAll("$1" + REDACTED);
        return KEY_VALUE_FIELD.matcher(redacted).replaceAll("$1" + REDACTED);
    }

    private static String redactJsonStringFields(String value) {
        StringBuilder output = null;
        int copyFrom = 0;
        int searchFrom = 0;
        while (searchFrom < value.length()) {
            int keyStart = value.indexOf('"', searchFrom);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = findJsonStringEnd(value, keyStart + 1);
            if (keyEnd < 0) {
                break;
            }
            if (!isSensitiveJsonKey(value, keyStart + 1, keyEnd)) {
                searchFrom = keyEnd + 1;
                continue;
            }

            int colon = skipWhitespace(value, keyEnd + 1);
            if (colon >= value.length() || value.charAt(colon) != ':') {
                searchFrom = keyEnd + 1;
                continue;
            }
            int valueQuote = skipWhitespace(value, colon + 1);
            if (valueQuote >= value.length() || value.charAt(valueQuote) != '"') {
                searchFrom = keyEnd + 1;
                continue;
            }

            int sensitiveValueStart = valueQuote + 1;
            int sensitiveValueEnd = findJsonStringEnd(value, sensitiveValueStart);
            if (output == null) {
                output = new StringBuilder(value.length());
            }
            output.append(value, copyFrom, sensitiveValueStart).append(REDACTED);
            if (sensitiveValueEnd < 0) {
                copyFrom = value.length();
                break;
            }
            copyFrom = sensitiveValueEnd;
            searchFrom = sensitiveValueEnd + 1;
        }
        if (output == null) {
            return value;
        }
        if (copyFrom < value.length()) {
            output.append(value, copyFrom, value.length());
        }
        return output.toString();
    }

    private static boolean isSensitiveJsonKey(String value, int start, int end) {
        return SENSITIVE_JSON_KEYS.contains(value.substring(start, end).toLowerCase(Locale.ROOT));
    }

    private static int skipWhitespace(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int findJsonStringEnd(String value, int start) {
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return index;
            }
        }
        return -1;
    }
}
