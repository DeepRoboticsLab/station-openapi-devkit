package com.deeprobotics.station.openapi.sample.config;

import com.deeprobotics.station.openapi.sample.safety.DangerousOperationInterceptor;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置 Sample 的双语消息解析和危险操作拦截器。
 *
 * <p>这里只组装轻量 Web 能力，不引入登录、Session 或生产权限体系。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SampleSafetyProperties.class)
public class SampleWebConfiguration implements WebMvcConfigurer {

    private final SampleSafetyProperties safetyProperties;

    public SampleWebConfiguration(SampleSafetyProperties safetyProperties) {
        this.safetyProperties = safetyProperties;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new SampleLocaleResolver();
    }

    /**
     * 显式注册仅含中英文资源的消息源，并在无法匹配语言时回退到中文。
     *
     * <p>Spring Boot 的默认自动配置要求存在无语言后缀的基础资源文件；Sample 刻意只保留
     * {@code zh_CN} 和 {@code en_US}，因此在这里直接声明消息源，避免运行时返回原始消息键。</p>
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器在 Controller 方法执行前检查标记，确保锁定操作不会进入 Service 或 SDK。
        registry.addInterceptor(new DangerousOperationInterceptor(safetyProperties));
    }
}
