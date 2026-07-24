package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 巡检结果和资源下载 API 分组。
 *
 * <p>结果分页是 READ_ONLY；资源下载是 FILE_DOWNLOAD。下载接口 {@code /remoteApi/result/resource}
 * 正常成功时返回二进制流，不是 {@code R<T>}。如果平台返回 JSON，SDK 会将其作为错误或协议异常处理。</p>
 *
 * <p>Path 下载重载先写入目标目录下的临时文件，成功后再移动到最终文件，避免调用方拿到不完整文件。</p>
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
     * 下载地图文件或巡检结果资源，并把完整二进制内容写入调用方提供的输出流。
     *
     * <p>对应 {@code POST /remoteApi/result/resource}。该接口成功时返回二进制流，不返回 {@code R<T>}；
     * 如果平台返回 JSON 失败包，SDK 会解析为业务异常，并且不会把 JSON 错误体写入调用方输出流。</p>
     *
     * @param request 下载请求，必须包含非空 {@code filePath} 和合法 {@code type}（1=地图文件，2=结果资源）
     * @param outputStream 调用方输出流，SDK 不负责关闭该流
     * @throws StationOpenApiDownloadException 下载响应无效，或临时文件、调用方输出流写入失败
     */
    void downloadResource(ResourceDownloadRequest request, OutputStream outputStream);

    /**
     * 下载资源到输出流，并为本次调用指定请求级选项。
     *
     * <p>FILE_DOWNLOAD 默认允许连接类失败重试。为避免失败重试污染调用方输出流，执行器会先把每次尝试写入
     * SDK 临时文件，只有某一次尝试完整成功并确认不是 JSON 错误包后，才复制到 {@code outputStream}。</p>
     *
     * @param request 下载请求，不能为 {@code null}
     * @param outputStream 调用方输出流，不能为 {@code null}，SDK 不负责关闭
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @throws StationOpenApiDownloadException 下载响应无效，或临时文件、调用方输出流写入失败
     */
    void downloadResource(ResourceDownloadRequest request, OutputStream outputStream, RequestOptions options);

    /**
     * 下载资源到本地目标文件。
     *
     * <p>SDK 会在目标目录原子创建本次调用独占的 {@code station-openapi-download-*.part} 临时文件，
     * 下载成功后再移动到最终 {@code target}。如果下载、写入或移动失败，会尽力删除这份 SDK 自建文件；
     * SDK 只管理本次调用随机创建的文件，不会复用或删除客户自行创建的其他旁路文件。</p>
     *
     * @param request 下载请求，必须包含非空 {@code filePath} 和合法 {@code type}
     * @param target 最终目标文件路径，必须包含文件名，不能是文件系统根目录；父目录不存在时 SDK 会创建
     * @return 最终目标文件的绝对路径
     * @throws StationOpenApiDownloadException 下载响应无效，或临时文件、目标文件写入或移动失败
     */
    Path downloadResource(ResourceDownloadRequest request, Path target);

    /**
     * 下载资源到本地目标文件，并为本次调用指定请求级选项。
     *
     * <p>移动临时文件时优先使用原子移动；如果当前文件系统不支持原子移动，会退回到普通替换移动。
     * 写入目标文件过程中的本地 I/O 失败会抛出下载异常，客户可根据异常上下文定位 path/requestId。</p>
     *
     * @param request 下载请求，不能为 {@code null}
     * @param target 最终目标文件路径，不能为 {@code null} 或文件系统根目录
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 最终目标文件的绝对路径
     * @throws StationOpenApiDownloadException 下载响应无效，或临时文件、目标文件写入或移动失败
     */
    Path downloadResource(ResourceDownloadRequest request, Path target, RequestOptions options);
}
