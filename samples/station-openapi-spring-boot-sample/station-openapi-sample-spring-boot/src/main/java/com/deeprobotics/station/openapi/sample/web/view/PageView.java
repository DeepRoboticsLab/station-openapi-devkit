package com.deeprobotics.station.openapi.sample.web.view;

import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import java.util.List;

/**
 * 面向 JSON 与 Swagger 的稳定分页响应视图。
 *
 * <p>该视图显式暴露 SDK record 风格访问器对应的属性；部分平台接口只返回记录和总数时，
 * 页码、页容量和总页数允许为 {@code null}。</p>
 */
public record PageView<T>(
        List<T> records,
        long total,
        Long pageNo,
        Long pageSize,
        Long pages) {

    /**
     * 将 SDK 分页结果转换为稳定的 Web 属性，同时保留可能缺失的分页元数据。
     */
    public static <T> PageView<T> from(PageResult<T> source) {
        return new PageView<>(
                source.records(),
                source.total(),
                source.pageNo(),
                source.pageSize(),
                source.pages());
    }
}
