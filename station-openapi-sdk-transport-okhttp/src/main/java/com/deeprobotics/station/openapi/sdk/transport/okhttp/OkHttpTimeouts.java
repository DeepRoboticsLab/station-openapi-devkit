package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.Duration;

/**
 * 把公共 Java {@link Duration} 统一转换为 OkHttp 可接受的毫秒范围。
 *
 * <p>Factory 建连超时与每次调用的 read/call timeout 必须共用这里的规则，避免同一个非法值在不同
 * 入口泄漏 NPE、IAE 或 ArithmeticException，甚至在危险操作上被误判为已发网后的结果未知。</p>
 */
final class OkHttpTimeouts {

    /** OkHttp 的毫秒超时最终存储为 int，最大值约为 24.8 天。 */
    private static final long MAX_TIMEOUT_MILLIS = Integer.MAX_VALUE;

    private OkHttpTimeouts() {
    }

    /**
     * 校验并转换一个已命名的超时字段；所有失败都属于尚未发网的客户参数错误。
     */
    static long toMillis(Duration duration, String fieldName) {
        if (duration == null) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new StationOpenApiClientException(fieldName + " must be positive");
        }

        long millis;
        try {
            millis = Math.max(1L, duration.toMillis());
        } catch (ArithmeticException ex) {
            throw new StationOpenApiClientException(fieldName + " exceeds the OkHttp timeout range");
        }
        if (millis > MAX_TIMEOUT_MILLIS) {
            throw new StationOpenApiClientException(
                    fieldName + " must not exceed " + MAX_TIMEOUT_MILLIS + " milliseconds");
        }
        return millis;
    }
}
