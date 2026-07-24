package com.deeprobotics.station.openapi.sample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 控制 Sample 中写入和设备控制示例是否允许执行。
 *
 * <p>这只是防止 Swagger 误操作的本地总开关，不能替代生产系统的认证、授权和审计。</p>
 */
@ConfigurationProperties(prefix = "sample")
public class SampleSafetyProperties {

    private boolean dangerousOperationsEnabled;

    public boolean isDangerousOperationsEnabled() {
        return dangerousOperationsEnabled;
    }

    public void setDangerousOperationsEnabled(boolean dangerousOperationsEnabled) {
        this.dangerousOperationsEnabled = dangerousOperationsEnabled;
    }
}
