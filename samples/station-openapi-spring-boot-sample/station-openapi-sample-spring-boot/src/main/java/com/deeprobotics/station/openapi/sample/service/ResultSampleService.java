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
 * <p>下载目标由调用方提供，Sample 不在内存中缓冲完整文件，也不绕过 SDK 的下载错误处理。
 * 为保证网络重试不会把失败尝试的部分内容写入调用方输出流，SDK 会将每次网络尝试写入磁盘临时文件，
 * 成功后再复制到调用方输出流。</p>
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
        // Sample 不缓冲完整文件；SDK 先写入单次尝试的临时文件，成功后再复制到该输出流。
        client.result().downloadResource(request, outputStream, options);
    }
}
