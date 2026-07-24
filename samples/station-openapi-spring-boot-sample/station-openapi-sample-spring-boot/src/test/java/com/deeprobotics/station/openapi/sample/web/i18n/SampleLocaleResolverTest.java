package com.deeprobotics.station.openapi.sample.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SampleLocaleResolverTest {

    private final SampleLocaleResolver resolver = new SampleLocaleResolver();

    @Test
    void langParameterWinsOverAcceptLanguage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("lang", "zh_CN");
        request.addHeader("Accept-Language", "en-US");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void englishQueryParameterWinsOverChineseHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("lang", "en-US");
        request.addHeader("Accept-Language", "zh-CN");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.US);
    }

    @Test
    void acceptLanguageUsesFirstSupportedLocale() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr-FR,zh-CN;q=0.9,en-US;q=0.8");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void unsupportedLanguageFallsBackToChinese() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("lang", "fr_FR");
        request.addHeader("Accept-Language", "fr-FR");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void localeCannotBeStoredInSessionOrCookie() {
        assertThatThrownBy(() -> resolver.setLocale(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                Locale.US))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
