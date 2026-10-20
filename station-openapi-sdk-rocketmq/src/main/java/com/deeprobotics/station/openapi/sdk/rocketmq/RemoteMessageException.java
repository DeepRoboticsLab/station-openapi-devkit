package com.deeprobotics.station.openapi.sdk.rocketmq;

/**
 * RocketMQ 远程推送模块的运行时异常。
 *
 * <p>公共 API 不声明 RocketMQ 原生 checked exception，避免业务代码被迫依赖底层 client
 * 的异常类型。配置校验和订阅器启动失败会抛出该异常。消息解析或 handler 执行失败由
 * {@link RemoteMessageRouter} 转换为 {@link RemoteMessageConsumeResult#RECONSUME_LATER}，不会继续抛给调用方。</p>
 */
public class RemoteMessageException extends RuntimeException {

    /**
     * 创建不带底层原因的 MQ 模块异常。
     *
     * @param message 已脱敏或不含敏感数据的错误说明
     */
    public RemoteMessageException(String message) {
        super(message);
    }

    /**
     * 创建保留底层原因的 MQ 模块异常。
     *
     * @param message 已脱敏或不含敏感数据的错误说明
     * @param cause 订阅器启动失败原因
     */
    public RemoteMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
