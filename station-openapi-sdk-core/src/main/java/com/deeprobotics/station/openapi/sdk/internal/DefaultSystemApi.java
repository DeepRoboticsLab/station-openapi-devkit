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
 * <p>时间查询无业务参数：签名模式仅发送鉴权字段，Token 模式发送 {@code {}} 请求体及令牌头。</p>
 */
public final class DefaultSystemApi implements SystemApi {

    /** 每次构造独立的空业务请求体，供后续鉴权和序列化使用。 */
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
        // 保留平台时间文本，不转换为本地时间类型。
        return executor.executeRequiredData(GET_TIME_TEXT, EMPTY_REQUEST, new TypeRef<ApiResponse<String>>() {
        }, options);
    }
}
