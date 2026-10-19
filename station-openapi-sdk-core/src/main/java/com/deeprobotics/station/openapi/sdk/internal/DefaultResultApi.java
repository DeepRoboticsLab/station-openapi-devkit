package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.ResultApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.OutputStream;
import java.util.Objects;

/**
 * 巡检结果和文件资源下载 API 默认实现。
 *
 * <p>分页解析 {@code R<Page<...>>}；下载成功返回二进制流，错误响应可能为 JSON，
 * 由 {@link OpenApiExecutor#download} 处理。</p>
 */
public final class DefaultResultApi implements ResultApi {

    private static final EndpointDefinition PAGE_TASK_POINT_RESULTS = EndpointDefinition.of(
            "result.pageTaskPointResults", "/remoteApi/taskPointResultPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition DOWNLOAD_RESOURCE = EndpointDefinition.of(
            "result.downloadResource", "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD);

    private final OpenApiExecutor executor;

    /**
     * 创建巡检结果 API 实现。
     *
     * @param executor 已完成客户端级配置的执行器，不能为 {@code null}
     */
    public DefaultResultApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public PageResult<PatrolResultInfo> pageTaskPointResults(TaskPointResultPageRequest request) {
        return pageTaskPointResults(request, null);
    }

    @Override
    public PageResult<PatrolResultInfo> pageTaskPointResults(TaskPointResultPageRequest request,
            RequestOptions options) {
        TaskPointResultPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_TASK_POINT_RESULTS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<PatrolResultInfo>>>() {
        }, options);
    }

    @Override
    public void downloadResource(ResourceDownloadRequest request, OutputStream outputStream) {
        downloadResource(request, outputStream, null);
    }

    @Override
    public void downloadResource(ResourceDownloadRequest request, OutputStream outputStream, RequestOptions options) {
        ResourceDownloadRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        OutputStream requiredOutputStream = ClientInputs.requireNonNull(outputStream, "outputStream");
        executor.download(DOWNLOAD_RESOURCE, requiredRequest, requiredOutputStream, options);
    }

}
