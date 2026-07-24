package com.deeprobotics.station.openapi.sdk.rocketmq;

/**
 * 客户处理远程推送消息的函数式接口。
 *
 * <p>handler 正常返回表示消息处理成功，SDK 会 ack；handler 抛出异常表示客户侧处理失败，
 * SDK 会让 RocketMQ 后续重投。允许抛 checked exception，是为了让客户可以直接调用数据库、
 * 文件或业务服务而不必额外包装异常。</p>
 *
 * @param <T> 已按 tag 解析完成的消息模型类型
 */
@FunctionalInterface
public interface RemoteMessageHandler<T> {

    /**
     * 处理一条已经完成 UTF-8 JSON 解析的业务消息。
     *
     * @param message 与当前 tag 对应的消息模型，不会为 {@code null}
     * @throws Exception 任意处理失败；subscriber 会请求 RocketMQ 后续重投
     */
    void handle(T message) throws Exception;
}
