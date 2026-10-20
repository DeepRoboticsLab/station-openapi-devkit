package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateStatusRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;

/**
 * 任务模板 API 分组。
 *
 * <p>模板查询属于 READ_ONLY；模板创建、启停和删除属于 BUSINESS_WRITE，不自动重试。
 * 这是因为请求失败时模板可能已经被平台保存，SDK 不能假设重复提交是安全的。</p>
 *
 * <p>请求字段的 wire 形态固定为：{@code taskCodes} 是 JSON 数组，
 * {@code pointCodes/cycleMonth/cycleWeek} 是英文逗号字符串。</p>
 */
public interface TaskTemplateApi {

    /**
     * 新增任务模板但不立即下发执行。
     *
     * <p>对应 {@code POST /remoteApi/onlySaveTaskModel}，成功后返回平台生成的 {@code taskCode}。
     * 这是业务写入操作，不自动重试。</p>
     *
     * @param request 模板创建请求，必须包含任务名称和巡检点编码
     * @return 非 null 的任务模板编码 {@code taskCode}
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    String create(TaskTemplateCreateRequest request);

    /**
     * 带单次调用选项的任务模板新增。
     *
     * <p>{@link RequestOptions} 可用于设置 requestId 和超时。创建模板属于 BUSINESS_WRITE，不自动重试；
     * 如果发生超时或连接中断，异常会带 {@code resultUnknown=true}，调用方应先通过分页或详情接口确认是否已创建。</p>
     *
     * @param request 模板创建请求，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的任务模板编码 {@code taskCode}
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    String create(TaskTemplateCreateRequest request, RequestOptions options);

    /**
     * 启用或停用任务模板（平台 v1.3.3 起支持）。
     *
     * <p>对应 {@code POST /remoteApi/switchTaskModelStatus}。停用会清除模板下未执行的任务；
     * 启用不会自动下发，需另行调用 {@code task().issue(...)}。成功响应允许 data 为 null。</p>
     *
     * <p>这是业务写入，不自动重试。连接或超时异常带 {@code resultUnknown=true} 时，
     * 应先查询模板详情的 isEnable 和任务执行记录，再决定下一步操作。</p>
     *
     * @param request 任务模板编码和整数目标状态，不能为 {@code null}
     */
    void switchStatus(TaskTemplateStatusRequest request);

    /**
     * 带单次调用选项的模板启停；副作用和版本要求见 {@link #switchStatus(TaskTemplateStatusRequest)}。
     *
     * @param request 任务模板编码和目标状态：0 停用、1 启用
     * @param options 请求 ID 和超时覆盖配置；null 使用默认配置，重试选项不能使本操作自动重试
     */
    void switchStatus(TaskTemplateStatusRequest request, RequestOptions options);

    /**
     * 按任务模板编码批量删除模板。
     *
     * <p>对应 {@code POST /remoteApi/removeTaskModelByCodes}。请求体中的 {@code taskCodes}
     * 必须保持 JSON 数组形态，不能拼成逗号字符串。删除同时清除未执行任务；
     * 如需停止正在执行的任务，应通过任务控制或批量取消接口操作。</p>
     *
     * @param request 模板删除请求，必须包含至少一个任务模板编码
     */
    void deleteByCodes(TaskTemplateDeleteRequest request);

    /**
     * 带单次调用选项的任务模板删除。
     *
     * <p>删除模板同样属于 BUSINESS_WRITE，不自动重试。网络异常后调用方应先重新查询模板列表，
     * 再决定是否补偿处理，避免对已经删除的模板重复执行写操作。</p>
     *
     * @param request 模板删除请求，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     */
    void deleteByCodes(TaskTemplateDeleteRequest request, RequestOptions options);

    /**
     * 按任务模板编码查询模板详情。
     *
     * <p>对应 {@code POST /remoteApi/taskModelByCode}，属于只读查询。</p>
     *
     * @param request 模板详情请求，必须包含任务模板编码
     * @return 非 null 的任务模板详情
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    TaskTemplateInfo getByCode(TaskTemplateGetRequest request);

    /**
     * 带单次调用选项的任务模板详情查询。
     *
     * <p>详情查询是 READ_ONLY，适合用于写操作结果未知后的人工或程序化确认。</p>
     *
     * @param request 模板详情请求，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的任务模板详情
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    TaskTemplateInfo getByCode(TaskTemplateGetRequest request, RequestOptions options);

    /**
     * 分页查询任务模板。
     *
     * <p>对应 {@code POST /remoteApi/modelPage}。SDK 对外只使用 {@code pageNo/pageSize}，
     * 不输出历史 {@code pageNum} 字段。</p>
     *
     * @param request 模板分页查询条件，不能为 {@code null}
     * @return 非 null 的任务模板分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request);

    /**
     * 带单次调用选项的任务模板分页查询。
     *
     * <p>分页查询是 READ_ONLY。可结合 {@code taskName}、{@code patrolType} 等过滤条件确认模板当前状态。</p>
     *
     * @param request 模板分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的任务模板分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request, RequestOptions options);
}
