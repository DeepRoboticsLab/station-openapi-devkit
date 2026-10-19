package com.deeprobotics.station.openapi.sdk.http;

import java.util.Objects;

/**
 * 单个 OpenAPI 操作的元数据。
 *
 * <p>业务 API 方法不应在 executor 调用处散落 path 和重试语义，而应先定义为
 * EndpointDefinition。这样后续覆盖测试可以集中验证 28 个生产 path，危险操作也能统一阻止默认重试。</p>
 */
public final class EndpointDefinition {

    /**
     * SDK 操作名，进入异常上下文和调试日志。
     *
     * <p>命名建议使用 {@code 分组.方法}，例如 {@code inventory.pageDogs}。</p>
     */
    private final String operation;
    /**
     * 平台 OpenAPI path。构造时会归一化为以斜杠开头的相对 path。
     */
    private final String path;
    /**
     * 操作意图，决定默认重试次数和连接失败后的结果未知语义。
     */
    private final OperationIntent intent;

    private EndpointDefinition(String operation, String path, OperationIntent intent) {
        this.operation = requireText(operation, "operation");
        this.path = normalizePath(path);
        this.intent = Objects.requireNonNull(intent, "intent is required");
    }

    /**
     * 创建 endpoint 元数据。
     *
     * @param operation SDK 操作名，进入异常上下文
     * @param path 平台 {@code /remoteApi/*} path
     * @param intent 操作意图，决定默认重试和 resultUnknown 语义
     * @return 已校验并归一化 path 的接口元数据
     */
    public static EndpointDefinition of(String operation, String path, OperationIntent intent) {
        return new EndpointDefinition(operation, path, intent);
    }

    /**
     * 返回进入异常和日志上下文的 SDK 操作名。
     *
     * @return SDK 业务操作标识
     */
    public String operation() {
        return operation;
    }

    /**
     * 返回归一化后的平台 path。
     *
     * @return 以 {@code /} 开头的平台 API path
     */
    public String path() {
        return path;
    }

    /**
     * 返回决定重试和结果未知语义的操作意图。
     *
     * @return 操作意图
     */
    public OperationIntent intent() {
        return intent;
    }

    @Override
    public String toString() {
        return "EndpointDefinition{"
                + "operation='" + operation + '\''
                + ", path='" + path + '\''
                + ", intent=" + intent
                + '}';
    }

    private static String normalizePath(String path) {
        String normalized = requireText(path, "path");
        // 统一保留前导斜杠，便于异常、测试和 EndpointResolver 输入保持一致。
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
