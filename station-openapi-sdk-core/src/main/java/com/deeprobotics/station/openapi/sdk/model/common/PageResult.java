package com.deeprobotics.station.openapi.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * SDK 统一分页结果。
 *
 * <p>平台分页响应存在两种字段组合：完整响应提供 {@code current/size/pages}，精简响应只提供
 * {@code records/total}。因此 {@code pageNo/pageSize/pages} 允许为空，使
 * {@code /remoteApi/streamPage} 等接口不会因缺少页码字段而解析失败。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PageResult<T> {

    /**
     * 当前页记录列表。缺失时按空列表处理，避免调用方遍历前判空。
     */
    private final List<T> records;
    /**
     * 平台返回的总记录数。缺失时归一为 0。
     */
    private final long total;
    /**
     * 当前页码。自定义分页返回可能缺失，因此允许为 {@code null}。
     */
    private final Long pageNo;
    /**
     * 每页数量。自定义分页返回可能缺失，因此允许为 {@code null}。
     */
    private final Long pageSize;
    /**
     * 总页数。自定义分页返回可能缺失，因此允许为 {@code null}。
     */
    private final Long pages;

    /**
     * 创建统一分页结果；缺失 records/total 分别归一为空列表和 0。
     *
     * @param records 当前页记录，缺失时按空列表处理
     * @param total 总记录数，缺失时按 0 处理
     * @param pageNo 平台 current 字段，精简分页响应可能缺失
     * @param pageSize 平台 size 字段，精简分页响应可能缺失
     * @param pages 平台 pages 字段，精简分页响应可能缺失
     */
    @JsonCreator
    public PageResult(@JsonProperty("records") List<T> records, @JsonProperty("total") Long total,
            @JsonProperty("current") Long pageNo, @JsonProperty("size") Long pageSize,
            @JsonProperty("pages") Long pages) {
        // records 缺失时按空列表处理，调用方遍历时不需要额外判空。
        this.records = records == null ? List.of() : List.copyOf(records);
        this.total = total == null ? 0L : total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.pages = pages;
    }

    /**
     * 返回不可变的当前页记录。
     *
     * @return 非 null 的不可变记录列表
     */
    public List<T> records() {
        return records;
    }

    /**
     * 返回总记录数。
     *
     * @return 总记录数，平台响应缺失时为 0
     */
    public long total() {
        return total;
    }

    /**
     * 返回当前页码；平台精简分页响应未提供时为 {@code null}。
     *
     * @return 当前页码或 {@code null}
     */
    public Long pageNo() {
        return pageNo;
    }

    /**
     * 返回每页数量；平台精简分页响应未提供时为 {@code null}。
     *
     * @return 每页数量或 {@code null}
     */
    public Long pageSize() {
        return pageSize;
    }

    /**
     * 返回总页数；平台精简分页响应未提供时为 {@code null}。
     *
     * @return 总页数或 {@code null}
     */
    public Long pages() {
        return pages;
    }

    @Override
    public String toString() {
        // 不展开 records，避免分页里包含敏感字段或巨大响应体时污染日志。
        return "PageResult{"
                + "recordCount=" + records.size()
                + ", total=" + total
                + ", pageNo=" + pageNo
                + ", pageSize=" + pageSize
                + ", pages=" + pages
                + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PageResult<?> that)) {
            return false;
        }
        return total == that.total
                && Objects.equals(records, that.records)
                && Objects.equals(pageNo, that.pageNo)
                && Objects.equals(pageSize, that.pageSize)
                && Objects.equals(pages, that.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(records, total, pageNo, pageSize, pages);
    }
}
