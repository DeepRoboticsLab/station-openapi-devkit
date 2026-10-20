package com.deeprobotics.station.openapi.sample.web.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Locale;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 为 Sample 选择简体中文或英文响应消息。
 *
 * <p>查询参数 {@code lang} 优先，其次读取 {@code Accept-Language}，无法识别时回退到简体中文。</p>
 */
public class SampleLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 显式 lang 便于 Swagger 和脚本稳定复现某种语言，优先级高于浏览器 Header。
        Locale queryLocale = supportedQueryLocale(request.getParameter("lang"));
        if (queryLocale != null) {
            return queryLocale;
        }

        // 按 Servlet 容器解析的 Accept-Language 优先级选择首个支持项。
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = locales.nextElement();
            if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
                return Locale.US;
            }
            if (Locale.CHINESE.getLanguage().equals(locale.getLanguage())) {
                return Locale.SIMPLIFIED_CHINESE;
            }
        }
        return Locale.SIMPLIFIED_CHINESE;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // Sample 不使用 Session 或 Cookie 保存语言，避免引入与 SDK 演示无关的状态。
        throw new UnsupportedOperationException("The Sample does not store locale state");
    }

    private static Locale supportedQueryLocale(String requested) {
        if ("en_US".equalsIgnoreCase(requested) || "en-US".equalsIgnoreCase(requested)) {
            return Locale.US;
        }
        if ("zh_CN".equalsIgnoreCase(requested) || "zh-CN".equalsIgnoreCase(requested)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return null;
    }
}
