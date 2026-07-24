package com.deeprobotics.station.openapi.sdk.auth;

/**
 * SDK 时间源。
 *
 * <p>生产环境使用系统时间生成 timestamp；测试环境可以注入固定时间，稳定签名向量。
 * timestamp 会进入签名源和请求 body，因此不要在签名后再修改。</p>
 */
@FunctionalInterface
public interface SdkClock {

    /**
     * 返回当前毫秒时间戳，按平台签名契约以字符串形式发送。
     *
     * @return 当前时间的 Unix 毫秒时间戳
     */
    long currentTimeMillis();

    /**
     * 返回基于 {@link System#currentTimeMillis()} 的生产时间源。
     *
     * @return 使用系统时钟的时间源
     */
    static SdkClock system() {
        return System::currentTimeMillis;
    }
}
