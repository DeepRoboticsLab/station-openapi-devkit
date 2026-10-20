package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sdk.RequestOptions;

/**
 * 把可选的 HTTP {@code X-Request-Id} 转换为 SDK {@link RequestOptions}。
 *
 * <p>调用方未提供请求 ID 时保持空值，由 SDK 统一生成，Sample 不重复实现生成规则。</p>
 */
public final class SampleRequestOptions {

    private SampleRequestOptions() {
    }

    /**
     * 构建单次 SDK 调用选项；请求 ID 的首尾空白和空值处理由 SDK 完成。
     */
    public static RequestOptions from(String requestId) {
        return RequestOptions.builder().requestId(requestId).build();
    }
}
