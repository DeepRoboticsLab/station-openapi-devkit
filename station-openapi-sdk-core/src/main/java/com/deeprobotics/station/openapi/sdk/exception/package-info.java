/**
 * SDK 对客户统一暴露的异常体系与脱敏工具。
 *
 * <p>异常上下文统一包含可获得的 requestId、path、operation、HTTP/业务 code 和 resultUnknown。
 * {@code resultUnknown=true} 表示写入或控制操作可能已经生效但客户端未收到确认，不能理解为明确失败。</p>
 *
 * <p>所有可能来自 HTTP header、响应片段、第三方异常或对象字符串的内容，在进入客户可见异常前都应经过
 * {@code Redactor}。secretKey、完整 token、sign 和摄像机 password 不得出现在异常、日志和字符串输出中。</p>
 */
package com.deeprobotics.station.openapi.sdk.exception;
