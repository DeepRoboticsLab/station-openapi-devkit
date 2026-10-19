package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.OutputStream;

/**
 * 巡检结果和资源下载 API 分组。
 *
 * <p>结果分页是 READ_ONLY；资源下载是 FILE_DOWNLOAD。下载接口 {@code /remoteApi/result/resource}
 * 正常成功时返回二进制流，不是 {@code R<T>}。如果平台返回 JSON，SDK 会将其作为错误或协议异常处理。</p>
 *
 * <p>下载直接写入调用方输出流，不创建临时文件、不完整缓存文件，也不自动重试。</p>
 */
public interface ResultApi {

    /**
     * 分页查询一次任务执行下的巡检点结果。
     *
     * <p>对应 {@code POST /remoteApi/taskPointResultPage}。{@code taskExecuteCode} 是平台必填字段；
     * 返回记录里的 {@code fileList.filePath} 可继续传给 {@link #downloadResource(ResourceDownloadRequest,
     * OutputStream)} 下载原始图片、地图或其他结果资源。</p>
     *
     * @param request 巡检点结果分页查询条件，必须包含非空 {@code taskExecuteCode}
     * @return 非 null 的巡检点结果分页数据；未知响应字段会被忽略
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<PatrolResultInfo> pageTaskPointResults(TaskPointResultPageRequest request);

    /**
     * 分页查询巡检点结果，并为本次调用指定请求级选项。
     *
     * <p>这是 READ_ONLY 操作，默认可按查询策略重试；每次重试都会重新生成鉴权信息，避免签名 nonce
     * 被复用。分页字段仍只发送 {@code pageNo/pageSize}，不会发送历史 {@code pageNum}。</p>
     *
     * @param request 巡检点结果分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的巡检点结果分页数据
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<PatrolResultInfo> pageTaskPointResults(TaskPointResultPageRequest request, RequestOptions options);

    /**
     * 流式下载地图文件或巡检结果资源。
     *
     * <p>对应 {@code POST /remoteApi/result/resource}。成功响应按块写入输出流；JSON 失败包解析为业务异常，
     * 不写入输出流。下载只尝试一次；中途失败可能留下部分数据，由调用方丢弃不完整输出后重新下载。</p>
     *
     * @param request 下载请求，必须包含非空 {@code filePath} 和合法 {@code type}（1=地图文件，2=结果资源）
     * @param outputStream 调用方输出流，SDK 不负责关闭
     * @throws StationOpenApiDownloadException 下载响应无效或输出流写入失败
     */
    void downloadResource(ResourceDownloadRequest request, OutputStream outputStream);

    /**
     * 流式下载资源，并指定本次调用的请求 ID 和超时。
     *
     * <p>无论 {@code options.retryPolicy} 为何值，下载都只尝试一次。连接、超时或输出失败时，
     * 异常的 {@code resultUnknown=true} 表示输出可能不完整，不能继续向同一输出追加重试结果。</p>
     *
     * @param request 下载请求，不能为 {@code null}
     * @param outputStream 调用方输出流，不能为 {@code null}，SDK 不负责关闭
     * @param options 本次调用的请求选项；传 {@code null} 时使用客户端默认配置
     * @throws StationOpenApiDownloadException 下载响应无效或输出流写入失败
     */
    void downloadResource(ResourceDownloadRequest request, OutputStream outputStream, RequestOptions options);
}
