package com.deeprobotics.station.openapi.sdk.http;

/**
 * OpenAPI 操作意图。
 *
 * <p>它不是展示给调用方的业务枚举，而是 SDK 内部的安全策略标签。
 * executor 根据它决定默认最大尝试次数，以及连接/超时失败时是否把结果标记为未知。</p>
 */
public enum OperationIntent {
    /**
     * 纯查询接口。默认最多尝试 2 次。
     */
    READ_ONLY(2, false),
    /**
     * 文件下载。只尝试一次；连接中断或超时可能留下不完整输出。
     */
    FILE_DOWNLOAD(1, true),
    /**
     * 业务写入，例如创建、启停和删除任务模板。不自动重试。
     */
    BUSINESS_WRITE(1, true),
    /**
     * 任务控制，例如下发、暂停、恢复、取消。不自动重试。
     */
    TASK_CONTROL(1, true),
    /**
     * 设备控制，例如四足狗、摄像机、云台、充电控制。不自动重试。
     */
    DEVICE_CONTROL(1, true);

    /**
     * 默认最大尝试次数。查询为 2，下载、写入和控制为 1。
     */
    private final int defaultMaxAttempts;
    /**
     * 连接丢失或超时时是否需要告诉调用方“结果未知”。
     *
     * <p>写入和控制可能已经生效；下载可能已向调用方交付部分字节。</p>
     */
    private final boolean resultUnknownOnConnectionLoss;

    OperationIntent(int defaultMaxAttempts, boolean resultUnknownOnConnectionLoss) {
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.resultUnknownOnConnectionLoss = resultUnknownOnConnectionLoss;
    }

    /**
     * 返回包含首次调用在内的默认最大尝试次数。
     *
     * @return 查询为 2，下载/写入/控制为 1
     */
    public int defaultMaxAttempts() {
        return defaultMaxAttempts;
    }

    /**
     * 返回连接中断或超时后是否应把异常标记为结果未知。
     *
     * @return 下载、写入和控制返回 {@code true}，查询返回 {@code false}
     */
    public boolean resultUnknownOnConnectionLoss() {
        return resultUnknownOnConnectionLoss;
    }
}
