package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务模板请求模型的包内校验和格式化工具。
 *
 * <p>任务模板请求里既有 JSON 数组字段，也有“集合语义但 wire 类型是字符串”的字段。把这些规则集中在这里，
 * 可以避免维护者误把 {@code taskCodes} 拼成逗号字符串，或误把 {@code pointCodes} 发成 JSON 数组。</p>
 */
final class TaskTemplateRequestValidators {

    /**
     * 任务模板接口接收的日期时间格式。
     */
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 任务模板接口接收的本地时间格式。
     */
    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TaskTemplateRequestValidators() {
    }

    /**
     * 校验必填文本字段并返回 trim 后的值。
     *
     * <p>请求体和签名源都使用规范化后的值，避免客户输入前后空白导致签名和平台业务判断难以排查。</p>
     */
    static String requireText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 校验必填逗号字符串字段。
     *
     * <p>适用于 {@code pointCodes} 这类 wire 类型为 String、但业务上代表多个编码的字段。
     * 方法会去掉每个片段的前后空白并重新用英文逗号拼接，确保 wire 形态稳定。</p>
     */
    static String requireCommaText(String fieldName, String value) {
        String text = requireText(fieldName, value);
        List<String> values = splitAndTrim(text);
        if (values.isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return String.join(",", values);
    }

    /**
     * 把可选集合转成英文逗号字符串。
     *
     * <p>null 表示客户未设置该字段，返回 null 后由 {@code MapUtils.putIfNotNull} 省略；
     * 非 null 集合会过滤 null 和空白片段，避免生成多余逗号。</p>
     */
    static String joinOptional(Collection<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(","));
    }

    /**
     * 校验必填字符串列表并返回规范化副本。
     *
     * <p>适用于 {@code taskCodes}：它的 wire 类型是 JSON 数组，不能复用逗号字符串逻辑。</p>
     */
    static List<String> requireStringList(String fieldName, Collection<String> values) {
        return normalizeRequiredList(fieldName, values);
    }

    /**
     * 按平台接口要求格式化日期时间字段。
     */
    static String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    /**
     * 按平台接口要求格式化本地时间字段。
     */
    static String format(LocalTime value) {
        return value == null ? null : TIME_FORMATTER.format(value);
    }

    /**
     * 规范化必填列表：过滤 null、空字符串和全空白字符串，并保留客户传入顺序。
     */
    private static List<String> normalizeRequiredList(String fieldName, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if (normalized.isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return normalized;
    }

    /**
     * 将英文逗号字符串拆分为去空白后的片段。
     *
     * <p>这里不接受中文逗号自动转换，避免客户配置中混入不可见格式问题时 SDK 静默修正，导致排查困难。</p>
     */
    private static List<String> splitAndTrim(String value) {
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.toList());
    }
}
