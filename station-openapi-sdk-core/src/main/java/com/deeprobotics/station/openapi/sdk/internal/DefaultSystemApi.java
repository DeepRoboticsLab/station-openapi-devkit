package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.SystemApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import java.util.Objects;

/**
 * 系统辅助 API 默认实现。
 *
 * <p>系统分组只公开服务器时间查询。它虽然没有业务参数，但仍经过统一执行器，
 * 这样签名模式可以生成 {@code appKey/timestamp/nonce/sign}，Token 模式也可以发送空 JSON body
 * 和 {@code X-Access-Token} header。</p>
 */
public final class DefaultSystemApi implements SystemApi {

    /**
     * /remoteApi/time 的空业务请求。
     *
     * <p>这里刻意返回一个新的空 LinkedHashMap，而不是 {@code null}，因为执行器后续还需要在该 body
     * 上附加鉴权字段或序列化为 {@code {}}。如果改成 {@code null}，签名模式和 Token 模式都会出现
     * 请求形态不一致的问题。</p>
     */
    private static final OpenApiRequest EMPTY_REQUEST = MapUtils::newBody;
    private static final EndpointDefinition GET_TIME_TEXT =
            EndpointDefinition.of("system.getTimeText", "/remoteApi/time", OperationIntent.READ_ONLY);

    private final OpenApiExecutor executor;

    /**
     * 创建系统辅助 API 实现。
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultSystemApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public String getTimeText() {
        return getTimeText(null);
    }

    @Override
    public String getTimeText(RequestOptions options) {
        // 服务器时间是 R<String>，SDK 只解包并透传 data，不在 core 内做时间类型转换。
        return executor.executeRequiredData(GET_TIME_TEXT, EMPTY_REQUEST, new TypeRef<ApiResponse<String>>() {
        }, options);
    }
}
