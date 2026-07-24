package com.deeprobotics.station.openapi.sample.web.view;

/**
 * 将 SDK 返回的单个字符串包装为字段明确、便于 Swagger 展示的 JSON 响应。
 */
public record ValueResponse(String value) {
}
