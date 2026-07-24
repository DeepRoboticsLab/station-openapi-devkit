package com.deeprobotics.station.openapi.sample.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sample.config.SampleSafetyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class DangerousOperationInterceptorTest {

    @Test
    void disabledDangerousMethodIsRejected() throws Exception {
        SampleSafetyProperties properties = new SampleSafetyProperties();
        DangerousOperationInterceptor interceptor = new DangerousOperationInterceptor(properties);
        HandlerMethod method = handlerMethod("dangerous");

        assertThatThrownBy(() -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), method))
                .isInstanceOf(DangerousOperationDisabledException.class);
    }

    @Test
    void enabledDangerousMethodIsAllowed() throws Exception {
        SampleSafetyProperties properties = new SampleSafetyProperties();
        properties.setDangerousOperationsEnabled(true);
        DangerousOperationInterceptor interceptor = new DangerousOperationInterceptor(properties);

        assertThat(interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod("dangerous")))
                .isTrue();
    }

    @Test
    void safeMethodAndNonControllerHandlersAreAllowed() throws Exception {
        DangerousOperationInterceptor interceptor =
                new DangerousOperationInterceptor(new SampleSafetyProperties());

        assertThat(interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod("safe")))
                .isTrue();
        assertThat(interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
                .isTrue();
    }

    private static HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
        return new HandlerMethod(new ProbeController(), ProbeController.class.getDeclaredMethod(name));
    }

    private static final class ProbeController {

        @DangerousSampleOperation
        void dangerous() {
        }

        void safe() {
        }
    }
}
