/**
 * SDK 自有的 wire 模型基础设施。
 *
 * <p>请求模型实现 {@code OpenApiRequest.toMap()}，显式控制字段名、顺序、空值、数组和逗号字符串形态；
 * 返回的 Map 同时作为鉴权输入和最终 JSON body 来源。响应模型忽略未知字段，优先保持平台原始编码和文本，
 * 避免 SDK 对现场字典做不稳定推断。</p>
 */
package com.deeprobotics.station.openapi.sdk.model;
