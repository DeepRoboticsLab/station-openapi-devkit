package com.deeprobotics.station.openapi.sample.safety;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记会写入平台、修改任务或控制现场设备的 Sample 方法。
 *
 * <p>被标记的方法默认由 {@link DangerousOperationInterceptor} 阻断，必须显式开启安全开关。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DangerousSampleOperation {
}
