package com.deeprobotics.station.openapi.sdk.http;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * endpoint 与 OpenAPI path 的 URI 解析器。
 *
 * <p>SDK 允许 endpoint 包含 context path，例如 {@code http://host:23282/web}。
 * 这里必须使用 URI 结构化处理，不能用字符串拼接，否则容易在尾斜杠、非法字符、query/fragment
 * 或 context path 场景产生难排查的问题。</p>
 */
public final class EndpointResolver {

    private EndpointResolver() {
    }

    /**
     * 将客户配置的 endpoint 和接口 path 解析成最终 URI。
     *
     * <p>这里保留 endpoint 的 scheme、userInfo、host、port 和 context path，只把 OpenAPI path
     * 追加到 context path 后面。SDK 不支持在 endpoint 或接口 path 中配置 query 和 fragment。</p>
     *
     * @param endpoint 客户配置的服务根地址，可包含 context path
     * @param path 相对于 endpoint 的平台 API path
     * @return 不含 query 和 fragment 的最终请求 URI
     */
    public static URI resolve(String endpoint, String path) {
        URI endpointUri = parseUri(requireText(endpoint, "endpoint"), "endpoint");
        URI pathUri = parseUri(requireText(path, "path"), "path");

        if (endpointUri.getScheme() == null || endpointUri.getHost() == null) {
            throw new StationOpenApiClientException("endpoint must include scheme and host");
        }
        rejectQueryOrFragment(endpointUri, "endpoint");
        if (pathUri.isAbsolute() || pathUri.getAuthority() != null) {
            throw new StationOpenApiClientException("path must be relative to endpoint");
        }
        rejectQueryOrFragment(pathUri, "path");

        try {
            // 使用 URI 构造器重新组装，保留 scheme/userInfo/host/port，同时明确丢弃 query 和 fragment。
            return new URI(endpointUri.getScheme(), endpointUri.getUserInfo(), endpointUri.getHost(),
                    endpointUri.getPort(), combinePaths(endpointUri.getPath(), pathUri.getPath()), null, null);
        } catch (URISyntaxException ex) {
            throw new StationOpenApiClientException("failed to resolve endpoint path", ex);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    private static URI parseUri(String value, String fieldName) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new StationOpenApiClientException(fieldName + " is invalid", ex);
        }
    }

    private static void rejectQueryOrFragment(URI uri, String fieldName) {
        // query/fragment 会改变最终请求语义，SDK 不支持在 endpoint/path 中配置。
        if (uri.getRawQuery() != null) {
            throw new StationOpenApiClientException(fieldName + " query is not supported");
        }
        if (uri.getRawFragment() != null) {
            throw new StationOpenApiClientException(fieldName + " fragment is not supported");
        }
    }

    private static String combinePaths(String endpointPath, String apiPath) {
        List<String> segments = new ArrayList<>();
        addPath(segments, endpointPath);
        addPath(segments, apiPath);
        // 即使 endpoint 没有 contextPath，也始终返回以 / 开头的绝对 path。
        return "/" + String.join("/", segments);
    }

    private static void addPath(List<String> segments, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        String normalized = path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.isBlank()) {
            segments.add(normalized);
        }
    }
}
