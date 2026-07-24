package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * 按确定性规则发现默认 {@link HttpTransportProvider}。
 *
 * <p>候选数量必须恰好为一个。先读取 provider 类型再实例化唯一候选，避免冲突场景执行任意 provider
 * 构造器及其潜在副作用。</p>
 */
public final class HttpTransportProviderLoader {

    private HttpTransportProviderLoader() {
    }

    /**
     * 优先使用线程上下文 ClassLoader；为空时回退到 SPI 类型自身的 ClassLoader。
     *
     * @return classpath 中唯一的 transport provider 实例
     */
    public static HttpTransportProvider load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = HttpTransportProvider.class.getClassLoader();
        }
        return load(classLoader);
    }

    static HttpTransportProvider load(ClassLoader classLoader) {
        List<ServiceLoader.Provider<HttpTransportProvider>> candidates;
        try {
            candidates = ServiceLoader.load(HttpTransportProvider.class, classLoader)
                    .stream()
                    .toList();
        } catch (ServiceConfigurationError | RuntimeException | LinkageError failure) {
            throw loadFailure(failure);
        }

        if (candidates.isEmpty()) {
            throw new StationOpenApiClientException(
                    "No HttpTransportProvider found. Add station-openapi-sdk or inject HttpTransport explicitly.");
        }

        if (candidates.size() > 1) {
            List<String> providerClasses;
            try {
                providerClasses = candidates.stream()
                        .map(ServiceLoader.Provider::type)
                        .map(Class::getName)
                        .sorted()
                        .toList();
            } catch (ServiceConfigurationError | RuntimeException | LinkageError failure) {
                throw loadFailure(failure);
            }
            throw new StationOpenApiClientException(
                    "Multiple HttpTransportProvider implementations found; inject HttpTransport explicitly: "
                            + providerClasses);
        }

        try {
            return candidates.get(0).get();
        } catch (ServiceConfigurationError | RuntimeException | LinkageError failure) {
            throw loadFailure(failure);
        }
    }

    private static StationOpenApiClientException loadFailure(Throwable failure) {
        return new StationOpenApiClientException(
                "Failed to load HttpTransportProvider. Check META-INF/services registration or inject "
                        + "HttpTransport explicitly.",
                failure);
    }
}
