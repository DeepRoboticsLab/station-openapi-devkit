package com.deeprobotics.station.openapi.sample.safety;

import com.deeprobotics.station.openapi.sample.config.SampleSafetyProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在危险 Controller 方法执行前应用本地防误触门禁。
 *
 * <p>门禁关闭时直接抛出本地异常，确保请求不会进入 Service，更不会触发 SDK 调用。</p>
 */
public class DangerousOperationInterceptor implements HandlerInterceptor {

    private final SampleSafetyProperties safetyProperties;

    public DangerousOperationInterceptor(SampleSafetyProperties safetyProperties) {
        this.safetyProperties = safetyProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只检查带明确标记的 Controller 方法，普通查询和 Spring 内部 Handler 不受影响。
        if (handler instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(DangerousSampleOperation.class)
                && !safetyProperties.isDangerousOperationsEnabled()) {
            throw new DangerousOperationDisabledException();
        }
        return true;
    }
}
