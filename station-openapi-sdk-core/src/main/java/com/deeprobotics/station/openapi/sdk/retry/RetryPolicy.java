package com.deeprobotics.station.openapi.sdk.retry;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import java.util.Objects;

/**
 * SDK 内部重试策略。
 *
 * <p>{@link OperationIntent} 限定尝试次数：查询最多 2 次，下载/写入/控制 1 次；仅重试连接和超时异常。</p>
 */
public final class RetryPolicy {

    private final int maxAttempts;

    private RetryPolicy(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
    }

    /**
     * 根据操作意图创建 SDK 默认策略。
     *
     * @param intent 接口操作意图
     * @return 查询最多两次、下载/写入/控制一次的策略
     */
    public static RetryPolicy defaultFor(OperationIntent intent) {
        return new RetryPolicy(Objects.requireNonNull(intent, "intent is required").defaultMaxAttempts());
    }

    /**
     * 创建不自动重试的策略。
     *
     * @return 最大尝试次数为 1 的策略
     */
    public static RetryPolicy never() {
        return new RetryPolicy(1);
    }

    /**
     * 把客户请求级开关与接口安全边界合并为内部策略。
     *
     * <p>{@code DEFAULT} 只恢复接口自身的默认次数，不会开启下载、写入或控制操作的重试。</p>
     *
     * @param requestRetryPolicy 请求级重试开关
     * @param intent 接口操作意图
     * @return 可供 executor 使用的尝试次数策略
     */
    public static RetryPolicy from(RequestOptions.RetryPolicy requestRetryPolicy, OperationIntent intent) {
        if (requestRetryPolicy == RequestOptions.RetryPolicy.NEVER) {
            return never();
        }
        return defaultFor(intent);
    }

    /**
     * 返回包含首次调用在内的最大尝试次数。
     *
     * @return 最大尝试次数，最小为 1
     */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * 判断一次失败后是否还有次数且失败类型允许重试。
     *
     * @param completedAttempt 已完成并失败的尝试序号，从 1 开始
     * @param failure 本次失败
     * @return 仅在仍有次数且失败是超时或连接异常时返回 {@code true}
     */
    public boolean shouldRetry(int completedAttempt, RuntimeException failure) {
        return completedAttempt < maxAttempts && isRetriable(failure);
    }

    private static boolean isRetriable(RuntimeException failure) {
        // HTTP 4xx/5xx 和平台业务失败不在这里重试，因为平台已经给出明确结果。
        return failure instanceof HttpTimeoutException || failure instanceof HttpConnectionException;
    }
}
