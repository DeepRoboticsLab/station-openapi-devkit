/**
 * 客户可直接调用的业务域 API。
 *
 * <p>接口只暴露 SDK 自己的请求/响应模型和 {@code RequestOptions}。普通重载和带请求级选项的重载
 * 执行相同的请求流程。</p>
 *
 * <p>查询接口按 READ_ONLY 处理；文件下载按 FILE_DOWNLOAD 处理；模板写入、任务控制和设备控制
 * 分别使用 BUSINESS_WRITE、TASK_CONTROL、DEVICE_CONTROL。后三类操作默认不自动重试，
 * 网络结果不确定时异常会提示客户先查询状态或等待事件确认。</p>
 *
 * <p>下载同样不重试，直接写入调用方输出流；中途失败时应丢弃不完整输出后重新下载。</p>
 *
 * <p>所有异常均为
 * {@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiException StationOpenApiException}
 * 的非受检子类：</p>
 * <ul>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException
 *       StationOpenApiClientException}：本地配置或请求参数无效，请求通常尚未发出；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException
 *       StationOpenApiAuthException}：鉴权凭证或鉴权数据生成失败；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiHttpException
 *       StationOpenApiHttpException}：平台返回非 2xx HTTP 状态；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException
 *       StationOpenApiBusinessException}：平台返回明确的业务失败；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException
 *       StationOpenApiSerializationException}：请求或响应 JSON 无法按 SDK 契约处理；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException
 *       StationOpenApiTimeoutException} 和
 *       {@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException
 *       StationOpenApiTransportException}：请求超时或网络传输失败；</li>
 *   <li>{@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException
 *       StationOpenApiDownloadException}：下载响应无效或输出流写入失败。</li>
 * </ul>
 *
 * <p>异常会尽可能携带 {@code requestId/path/operation/code/resultUnknown}。写入或控制操作的
 * {@code resultUnknown=true} 表示操作可能已经生效但尚未得到确认，应先查询状态或等待 MQ 事件，
 * 不要立即重复提交。下载的此标记表示输出可能不完整。</p>
 */
package com.deeprobotics.station.openapi.sdk.api;
