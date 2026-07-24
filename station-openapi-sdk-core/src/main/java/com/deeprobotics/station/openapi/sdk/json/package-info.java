/**
 * SDK HTTP 链路使用的统一 JSON 编解码基础设施。
 *
 * <p>{@code JsonCodec} 固定 UTF-8、忽略未知响应字段，并统一处理 {@code yyyy-MM-dd HH:mm:ss} 与
 * {@code HH:mm:ss} 时间格式。请求字段是否出现仍由显式 {@code toMap()} 决定，不依赖反射导出模型字段。</p>
 *
 * <p>{@code TypeRef} 用于保留 {@code ApiResponse<PageResult<T>>} 等嵌套泛型信息；直接丢失泛型信息会使
 * Jackson 把业务数据解析成无类型 Map，因此 executor 的响应类型必须使用完整 TypeRef。</p>
 */
package com.deeprobotics.station.openapi.sdk.json;
