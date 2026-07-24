package com.deeprobotics.station.openapi.sdk.model;

import java.util.LinkedHashMap;

/**
 * 所有 OpenAPI 请求模型的统一契约。
 *
 * <p>请求模型不依赖平台内部请求类型，也不通过反射自动导出字段，而是显式实现 {@link #toMap()}。
 * 这样每个字段的 wire 名称、顺序、null 处理、数组或逗号字符串形态都能被单元测试固定。</p>
 */
public interface OpenApiRequest {

    /**
     * 转换为待鉴权和序列化的业务请求体。
     *
     * <p>返回 {@link LinkedHashMap} 是有意设计：签名虽然最终按 key 排序，
     * 但稳定插入顺序能让测试、日志和调试输出更容易阅读。实现类不要返回可被外部共享修改的内部 map。</p>
     *
     * @return 本次调用待鉴权、签名和 JSON 序列化的业务字段副本
     */
    LinkedHashMap<String, Object> toMap();
}
