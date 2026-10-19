/**
 * SDK 对客户统一暴露的异常体系与脱敏工具。
 *
 * <p>异常上下文统一包含可获得的 requestId、path、operation、HTTP/业务 code 和 resultUnknown。
 * {@code resultUnknown=true} 表示写入或控制操作可能已经生效但客户端未收到确认，不能理解为明确失败。</p>
 *
 * <p>HTTP 错误体及 JSON 解析/序列化异常中的原值不进入异常。业务消息和受控上下文仍经 {@code Redactor}
 * 处理；它只能识别已知字段形态，不能替代对原始报文的省略。secretKey、完整 token、sign 和摄像机密码
 * 不得出现在异常、日志和字符串输出中。</p>
 */
package com.deeprobotics.station.openapi.sdk.exception;
