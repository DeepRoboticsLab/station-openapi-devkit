package com.deeprobotics.station.openapi.sdk.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请求模型构造 wire body 的小工具。
 *
 * <p>这里集中处理 null、集合拷贝和逗号字符串，避免每个请求模型各写一套规则。
 * 这些规则会直接影响签名源和 JSON body，不能随意改动。</p>
 */
public final class MapUtils {

    private MapUtils() {
    }

    /**
     * 创建请求体 Map。
     *
     * <p>统一入口能提醒维护者：请求体应保持插入顺序稳定，不能随意换成 HashMap。</p>
     *
     * @return 新的空 {@link LinkedHashMap}
     */
    public static LinkedHashMap<String, Object> newBody() {
        return new LinkedHashMap<>();
    }

    /**
     * 仅在值非 null 时写入 body。
     *
     * <p>null 字段不发送，因为平台验签会跳过 null，SDK body 若保留 null 容易让调用方排查时误解。
     * 空字符串不在这里过滤：签名规则是“空白字符串不参与签名”，但部分字段可能仍需要发送空字符串。</p>
     *
     * @param body 待写入字段的请求体
     * @param key 非 null 的 wire 字段名
     * @param value 字段值；为 {@code null} 时不写入
     */
    public static void putIfNotNull(LinkedHashMap<String, Object> body, String key, Object value) {
        Objects.requireNonNull(body, "body is required");
        Objects.requireNonNull(key, "key is required");
        if (value != null) {
            body.put(key, copyValue(value));
        }
    }

    /**
     * 将调用方集合转换为平台接口要求的英文逗号字符串。
     *
     * <p>仅用于 wire 类型为 String、业务上表示集合的字段，例如 pointCodes、cycleMonth、
     * cycleWeek、statusArr。不要用于 taskCodes 或 taskExecuteCodeList，这两个字段 wire 形态必须是 JSON array。</p>
     *
     * @param body 待写入字段的请求体
     * @param key 非 null 的 wire 字段名
     * @param values 待连接集合；为 {@code null} 时不写入
     */
    public static void putCommaJoined(LinkedHashMap<String, Object> body, String key, Collection<?> values) {
        Objects.requireNonNull(body, "body is required");
        Objects.requireNonNull(key, "key is required");
        if (values != null) {
            body.put(key, values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
    }

    /**
     * 复制请求体并移除 null 值。
     *
     * <p>鉴权 provider 和鉴权后请求快照都会使用这条路径。集合会浅拷贝成新的 {@link ArrayList}，
     * 防止调用方在请求发出后修改原集合，导致日志、重试或签名排查时看到不一致的数据。</p>
     *
     * @param source 原始请求体，允许为 {@code null}
     * @return 已移除 null 值并复制集合的有序请求体
     */
    public static LinkedHashMap<String, Object> copyWithoutNulls(Map<String, ?> source) {
        LinkedHashMap<String, Object> target = newBody();
        if (source == null) {
            return target;
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            putIfNotNull(target, entry.getKey(), entry.getValue());
        }
        return target;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Collection<?> collection) {
            // 保持集合值稳定的 List.toString() 形态；平台验签兼容性测试依赖这一点。
            return new ArrayList<>(collection);
        }
        return value;
    }
}
