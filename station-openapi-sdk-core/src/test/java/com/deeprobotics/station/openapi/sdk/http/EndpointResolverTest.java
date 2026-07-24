package com.deeprobotics.station.openapi.sdk.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * endpoint 与 RemoteApi path 结构化拼接测试。
 *
 * <p>锁定尾斜杠、部署 context path、非法 URI、query 和 fragment 的处理，避免回退到容易产生双斜杠、
 * 丢失 context path 或意外携带查询参数的字符串拼接实现。</p>
 */
class EndpointResolverTest {

    @Test
    void resolvesRemoteApiPathAgainstEndpointWithoutTrailingSlash() {
        URI uri = EndpointResolver.resolve("http://host:23288", "/remoteApi/time");

        assertThat(uri).hasToString("http://host:23288/remoteApi/time");
    }

    @Test
    void resolvesRelativeRemoteApiPathAgainstEndpointWithTrailingSlash() {
        URI uri = EndpointResolver.resolve("http://host:23288/", "remoteApi/time");

        assertThat(uri).hasToString("http://host:23288/remoteApi/time");
    }

    @Test
    void preservesEndpointContextPathWhenResolvingRelativePath() {
        URI uri = EndpointResolver.resolve("http://host:23282/web", "remoteApi/time");

        assertThat(uri).hasToString("http://host:23282/web/remoteApi/time");
    }

    @Test
    void rejectsEndpointQueryAndFragment() {
        assertThatThrownBy(() -> EndpointResolver.resolve("http://host:23288?debug=true", "remoteApi/time"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("endpoint");

        assertThatThrownBy(() -> EndpointResolver.resolve("http://host:23288/#frag", "remoteApi/time"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("endpoint");
    }

    @Test
    void rejectsPathQueryAndFragment() {
        assertThatThrownBy(() -> EndpointResolver.resolve("http://host:23288", "remoteApi/time?debug=true"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("path");

        assertThatThrownBy(() -> EndpointResolver.resolve("http://host:23288", "remoteApi/time#frag"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("path");
    }

    @Test
    void invalidEndpointOrPathThrowsSdkClientException() {
        assertThatThrownBy(() -> EndpointResolver.resolve("http://[bad", "remoteApi/time"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("endpoint");

        assertThatThrownBy(() -> EndpointResolver.resolve("http://host:23288", "remoteApi/%zz"))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("path");
    }
}
