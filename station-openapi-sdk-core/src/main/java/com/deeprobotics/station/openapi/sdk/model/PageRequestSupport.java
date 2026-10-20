package com.deeprobotics.station.openapi.sdk.model;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * 分页请求的公共基类。
 *
 * <p>SDK 对外统一使用 {@code pageNo/pageSize}，不输出平台历史兼容的旧页码字段。
 * pageSize 做默认值和上限归一，既保护平台，也让调用方在未配置时拿到可预期的分页大小。</p>
 */
public abstract class PageRequestSupport implements OpenApiRequest {

    /**
     * SDK 分页默认页码。
     */
    public static final int DEFAULT_PAGE_NO = 1;
    /**
     * SDK 分页默认每页数量。
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * SDK 单页最大数量，防止调用方误传过大 pageSize。
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 写入 SDK 统一分页字段。
     *
     * @param body 待写入分页字段的业务请求体
     * @param pageNo 调用方设置的页码，{@code null} 使用默认值
     * @param pageSize 调用方设置的每页数量，{@code null} 使用默认值
     */
    protected final void putPage(LinkedHashMap<String, Object> body, Integer pageNo, Integer pageSize) {
        Objects.requireNonNull(body, "body is required");
        body.put("pageNo", normalizePageNo(pageNo));
        body.put("pageSize", normalizePageSize(pageSize));
    }

    /**
     * pageNo 缺省为 1；小于 1 是调用方输入错误，直接本地失败。
     *
     * @param pageNo 调用方设置的页码
     * @return 大于等于 1 的页码
     */
    protected final int normalizePageNo(Integer pageNo) {
        if (pageNo == null) {
            return DEFAULT_PAGE_NO;
        }
        if (pageNo < 1) {
            throw new StationOpenApiClientException("pageNo must be greater than or equal to 1");
        }
        return pageNo;
    }

    /**
     * pageSize 缺省和非正数都归一为 10，超过 100 截断为 100。
     *
     * @param pageSize 调用方设置的每页数量
     * @return 1 到 100 之间的分页大小
     */
    protected final int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
