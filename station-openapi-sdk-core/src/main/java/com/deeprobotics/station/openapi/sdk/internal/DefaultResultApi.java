package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.ResultApi;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    @Override
    public Path downloadResource(ResourceDownloadRequest request, Path target) {
        return downloadResource(request, target, null);
    }

    @Override
    public Path downloadResource(ResourceDownloadRequest request, Path target, RequestOptions options) {
        ResourceDownloadRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        RequestOptions effectiveOptions = executor.prepareOptions(options);
        Path targetPath = ClientInputs.requireNonNull(target, "target").toAbsolutePath();
        Path targetFileName = targetPath.getFileName();
        if (targetFileName == null) {
            // 在本地 I/O 和远端请求前拒绝以根目录作为目标文件。
            throw new StationOpenApiClientException("target must identify a file");
        }
        Path parent = targetPath.getParent();
        if (parent == null) {
            // 兼容有文件名但无父目录的自定义 Path 实现。
            parent = Path.of(".").toAbsolutePath().normalize();
            targetPath = parent.resolve(targetFileName);
        }
        Path partPath = null;
        try {
            /*
             * 在目标目录创建独立 .part 文件，完整下载后再替换目标，避免跨文件系统移动和半文件暴露。
             * 每次调用只清理自己的临时文件；同一目标的并发下载以最后完成替换的调用为准。
             */
            Files.createDirectories(parent);
            partPath = Files.createTempFile(parent, "station-openapi-download-", ".part");
            try (OutputStream outputStream = Files.newOutputStream(partPath)) {
                executor.download(DOWNLOAD_RESOURCE, requiredRequest, outputStream, effectiveOptions);
            }
            movePartToTarget(partPath, targetPath);
            return targetPath;
        } catch (RuntimeException | IOException ex) {
            deletePartQuietly(partPath);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            // 本地 I/O 失败不代表远端执行结果未知，保留请求 ID 便于关联排查。
            throw new StationOpenApiDownloadException("Failed to write OpenAPI download target file",
                    DOWNLOAD_RESOURCE.path(), effectiveOptions.requestId(), DOWNLOAD_RESOURCE.operation(), false, ex);
        }
    }

    /**
     * 优先原子替换目标文件；文件系统不支持时退回普通替换。
     */
    private static void movePartToTarget(Path partPath, Path targetPath) throws IOException {
        try {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 尽力删除临时文件，清理失败不覆盖原始下载或写入异常。
     */
    private static void deletePartQuietly(Path partPath) {
        if (partPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(partPath);
        } catch (IOException | RuntimeException ex) {
            // 保留原始异常。
        }
    }

}
