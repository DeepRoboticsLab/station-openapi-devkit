package com.deeprobotics.station.openapi.sdk.model.request.task;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务执行请求模型的包内校验和格式化工具。
 *
 * <p>这些工具不对外公开，是为了把“任务执行”分组的 wire 规则集中在一个包内位置：
 * 必填文本需要 trim、批量取消列表必须保留 JSON 数组、状态集合需要转成逗号字符串、日期必须是
 * {@code yyyy-MM-dd}。这样后续新增任务接口时能复用同一套契约规则。</p>
 */
final class TaskRequestValidators {

    /**
     * 平台任务分页日期字段要求的格式。
     */
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TaskRequestValidators() {
    }

    /**
     * 校验必填文本字段。
     *
     * <p>这些字段属于平台必填文本。SDK 在本地提前失败，可以避免无意义请求进入签名、重试和 HTTP 层，
     * 也能让调用方更快定位是入参问题而不是平台业务失败。</p>
     */
    static String requireText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 校验并归一必填字符串集合。
     *
     * <p>用于 {@code taskExecuteCodeList} 这类 wire 类型为数组的字段。返回值仍是 List，
     * 不能转换为逗号字符串，否则 wire contract 会从 JSON array 变成 String。</p>
     */
    static List<String> requireStringList(String fieldName, Collection<String> values) {
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
     * 将可选集合转换为平台 String 字段需要的英文逗号字符串。
     *
     * <p>用于 {@code statusArr} 这类“集合语义但 wire 类型是 String”的字段。null 表示调用方未设置，
     * 返回 null 后由请求模型省略字段；空白元素会被过滤。</p>
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
     * 将 SDK 日期类型格式化为平台查询字符串。
     *
     * <p>返回 null 表示调用方未设置日期过滤条件，请求模型会省略对应字段。</p>
     */
    static String format(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }
}
