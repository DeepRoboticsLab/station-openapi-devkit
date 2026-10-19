package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.OutputStream;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.result()} 分组的结果查询与资源下载。
 *
 * <p>下载直接写入调用方提供的输出流，不暂存、不重试，失败时输出可能不完整。</p>
 */
@Service
public class ResultSampleService {

    private final StationOpenApiClient client;

    public ResultSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public PageResult<PatrolResultInfo> pageTaskPointResults(
            TaskPointResultPageRequest request, RequestOptions options) {
        return client.result().pageTaskPointResults(request, options);
    }

    public void download(ResourceDownloadRequest request, OutputStream outputStream, RequestOptions options) {
        client.result().downloadResource(request, outputStream, options);
    }
}
