/**
 * SDK 内部装配与统一执行链路。
 *
 * <p>各 {@code Default*Api} 只把公共方法绑定到确定的 path、响应类型和操作意图；
 * {@code OpenApiExecutor} 是唯一负责请求映射、鉴权、JSON、transport 调用、重试、响应解包和异常转换的组件。
 * 这种收口避免业务分组各自实现安全策略。</p>
 *
 * <p>本包虽然因模块装配需要包含 public 类型，但不是客户兼容性承诺的一部分。客户应通过
 * {@code StationOpenApiClients} 和 {@code api} 包使用 SDK，不应直接实例化本包实现。</p>
 */
package com.deeprobotics.station.openapi.sdk.internal;
