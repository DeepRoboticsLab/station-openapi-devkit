package com.deeprobotics.station.openapi.sdk.rocketmq;

/**
 * 处理远程推送消息的函数式接口。
 *
 * <p>正常返回确认消费，抛出异常请求重投。原生订阅器会并发调用，处理器须线程安全并按业务键幂等。</p>
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
