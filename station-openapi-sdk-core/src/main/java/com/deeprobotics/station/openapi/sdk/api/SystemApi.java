package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;

/**
 * 系统辅助 API 分组。
 *
 * <p>系统分组只包含服务器时间接口 {@code /remoteApi/time}。该接口没有业务请求字段：
 * 签名模式发送鉴权字段，Token 模式发送空 JSON body 并携带 {@code X-Access-Token} header。
 * 两种模式都会发送 JSON body 并执行相应鉴权。</p>
 */
public interface SystemApi {

    /**
     * 获取服务器当前时间文本。
     *
     * <p>对应 {@code POST /remoteApi/time}，返回平台成功响应中的时间文本。
     * 返回值可能采用 {@code LocalDateTime.toString()} 形态，例如 {@code 2026-07-07T10:20:30.123}；
     * SDK 不强制转换为 {@code LocalDateTime}，避免因时区、毫秒精度或格式差异导致解析失败。</p>
     *
     * @return 非 null 的服务器时间字符串
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    String getTimeText();

    /**
     * 获取服务器当前时间文本，并为本次调用指定请求级选项。
     *
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的服务器时间字符串
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    String getTimeText(RequestOptions options);
}
