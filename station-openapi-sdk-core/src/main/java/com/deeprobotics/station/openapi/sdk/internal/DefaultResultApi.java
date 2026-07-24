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
 * <p>结果分页仍然走普通 {@code R<Page<...>>} 解析；资源下载接口是特殊路径，成功时返回二进制流，
 * 失败时才可能返回 JSON 错误包。因此下载方法必须走 {@link OpenApiExecutor#download}，
 * 不能复用 {@link OpenApiExecutor#execute} 的 {@code R<T>} 解包流程。</p>
 */
public final class DefaultResultApi implements ResultApi {

    /**
     * 查询一次任务执行下的巡检点结果。
     *
     * <p>返回记录中的 {@code fileList.filePath} 是下载接口的 filePath 入参来源，SDK 不在这里预取文件，
     * 避免分页查询被大文件下载拖慢或占用内存。</p>
     */
    private static final EndpointDefinition PAGE_TASK_POINT_RESULTS = EndpointDefinition.of(
            "result.pageTaskPointResults", "/remoteApi/taskPointResultPage", OperationIntent.READ_ONLY);
    /**
     * 下载地图或巡检结果资源。
     *
     * <p>该接口是 {@link OperationIntent#FILE_DOWNLOAD}，执行器会按下载语义处理重试、临时文件和 JSON 错误包。
     * 对外仍使用 {@code ResourceDownloadRequest.toMap()} 参与签名和 JSON 请求体构造。</p>
     */
    private static final EndpointDefinition DOWNLOAD_RESOURCE = EndpointDefinition.of(
            "result.downloadResource", "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD);

    /**
     * OpenAPI 执行器，集中处理鉴权、HTTP 传输、响应解析和下载异常上下文。
     */
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
        /*
         * taskExecuteCode 是平台接口必填字段，由请求模型本地校验。
         * READ_ONLY 查询可安全按查询重试策略执行，每次重试仍会重新签名，避免 nonce/sign 被复用。
         */
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
        /*
         * OutputStream 由调用方创建和关闭，SDK 只负责写入成功的二进制内容。
         * 如果平台返回 2xx JSON 错误包，执行器会解析并抛异常，不会把错误 JSON 写入调用方流。
         */
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
            // 文件系统根路径只表示目录，不能作为最终下载文件；在任何本地 I/O 或远端请求前拒绝。
            throw new StationOpenApiClientException("target must identify a file");
        }
        Path parent = targetPath.getParent();
        if (parent == null) {
            // 兼容“有文件名但没有父目录”的自定义 Path provider，统一归一到当前工作目录。
            parent = Path.of(".").toAbsolutePath().normalize();
            targetPath = parent.resolve(targetFileName);
        }
        Path partPath = null;
        try {
            /*
             * Path 下载采用“同目录唯一 .part 文件 -> 成功后移动”的两阶段写入：
             * 1. 同目录保证普通 move 和原子 move 不需要跨文件系统；
             * 2. 失败时客户不会看到半截目标文件；
             * 3. 临时文件由 Files.createTempFile 原子创建并使用随机文件名，SDK 只清理本次调用创建的文件，
             *    不会触碰客户自行创建的旁路文件，也不会让并发下载在临时阶段互相覆盖；
             * 4. 成功后替换目标文件，符合下载覆盖语义。同一 target 的并发调用最终仍是最后完成 move
             *    的调用生效，客户若要求固定顺序，应在业务层按 target 串行化。
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
            /*
             * 本地文件系统 I/O 失败不属于远端执行状态未知，resultUnknown 固定为 false。
             * requestId 透传给异常，方便客户把本地错误和一次 SDK 调用日志关联起来。
             */
            throw new StationOpenApiDownloadException("Failed to write OpenAPI download target file",
                    DOWNLOAD_RESOURCE.path(), effectiveOptions.requestId(), DOWNLOAD_RESOURCE.operation(), false, ex);
        }
    }

    /**
     * 把完整下载好的临时文件移动到最终目标路径。
     *
     * <p>优先使用原子移动，降低客户读取到中间状态文件的概率；如果目标文件系统不支持原子移动，
     * 再退回到普通替换移动，保证本地文件系统差异不会导致下载功能不可用。</p>
     */
    private static void movePartToTarget(Path partPath, Path targetPath) throws IOException {
        try {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 尽力删除下载临时文件。
     *
     * <p>清理失败只能说明本地文件系统或权限还有额外问题，不能覆盖最初的下载/写入异常，
     * 否则客户会丢失真正导致调用失败的上下文。</p>
     */
    private static void deletePartQuietly(Path partPath) {
        if (partPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(partPath);
        } catch (IOException | RuntimeException ex) {
            // 临时文件清理采用尽力而为策略，清理失败不能覆盖最初的下载或写入异常。
        }
    }

}
