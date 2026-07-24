package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.TaskTemplateApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
import java.util.Objects;

/**
 * 任务模板 API 默认实现。
 *
 * <p>创建和删除是业务写入，默认不重试；详情和分页是只读查询，可按查询策略处理连接类失败。</p>
 */
public final class DefaultTaskTemplateApi implements TaskTemplateApi {

    /**
     * 新增任务模板 endpoint。
     *
     * <p>创建模板会写入平台业务数据，超时后操作可能已经生效，因此必须使用 BUSINESS_WRITE：
     * 默认不重试，并在连接/读取异常时通过 {@code resultUnknown=true} 提醒客户做后续确认。</p>
     */
    private static final EndpointDefinition CREATE = EndpointDefinition.of(
            "taskTemplate.create", "/remoteApi/onlySaveTaskModel", OperationIntent.BUSINESS_WRITE);
    /**
     * 批量删除任务模板 endpoint。
     *
     * <p>删除是业务写操作，不能因为 HTTP 层没有拿到响应就自动再次删除。请求体的 {@code taskCodes}
     * 由请求模型保持为 JSON 数组。</p>
     */
    private static final EndpointDefinition DELETE_BY_CODES = EndpointDefinition.of(
            "taskTemplate.deleteByCodes", "/remoteApi/removeTaskModelByCodes", OperationIntent.BUSINESS_WRITE);
    /**
     * 任务模板详情 endpoint。
     *
     * <p>该接口只读取模板信息，可按 READ_ONLY 策略处理临时连接失败。</p>
     */
    private static final EndpointDefinition GET_BY_CODE = EndpointDefinition.of(
            "taskTemplate.getByCode", "/remoteApi/taskModelByCode", OperationIntent.READ_ONLY);
    /**
     * 任务模板分页 endpoint。
     *
     * <p>分页查询同样是 READ_ONLY。请求模型对外使用 {@code pageNo/pageSize}，
     * 并由 {@code PageRequestSupport} 负责默认值和上限归一。</p>
     */
    private static final EndpointDefinition PAGE =
            EndpointDefinition.of("taskTemplate.page", "/remoteApi/modelPage", OperationIntent.READ_ONLY);

    /**
     * 统一执行器，屏蔽鉴权、签名、JSON、重试、异常和 R<T> 解包细节。
     */
    private final OpenApiExecutor executor;

    /**
     * 创建任务模板 API 实现。
     *
     * <p>DefaultTaskTemplateApi 不保存任何业务状态；所有请求数据都来自不可变 request 对象。</p>
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultTaskTemplateApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public String create(TaskTemplateCreateRequest request) {
        // 所有无 options 重载都委托给完整方法，避免维护两套执行逻辑。
        return create(request, null);
    }

    @Override
    public String create(TaskTemplateCreateRequest request, RequestOptions options) {
        TaskTemplateCreateRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        /*
         * 平台响应的 data 是新模板的 taskCode。这里直接返回 String，
         * 让客户后续可以用于查询详情、删除模板或任务下发。
         */
        return executor.executeRequiredData(CREATE, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, options);
    }

    @Override
    public void deleteByCodes(TaskTemplateDeleteRequest request) {
        // 删除没有有意义的业务返回值，只需要确认 R<?> 成功。
        deleteByCodes(request, null);
    }

    @Override
    public void deleteByCodes(TaskTemplateDeleteRequest request, RequestOptions options) {
        TaskTemplateDeleteRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 使用 Object 承接可空 data，避免把平台空 data 暴露到公共 API。
        executor.execute(DELETE_BY_CODES, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public TaskTemplateInfo getByCode(TaskTemplateGetRequest request) {
        // 详情查询和分页查询都属于只读，具体重试策略由 endpoint intent 决定。
        return getByCode(request, null);
    }

    @Override
    public TaskTemplateInfo getByCode(TaskTemplateGetRequest request, RequestOptions options) {
        TaskTemplateGetRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // SDK 将平台详情响应映射为稳定的 TaskTemplateInfo 模型。
        return executor.executeRequiredData(GET_BY_CODE, requiredRequest,
                new TypeRef<ApiResponse<TaskTemplateInfo>>() {
        }, options);
    }

    @Override
    public PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request) {
        // 分页入口统一走完整方法，确保 pageNo/pageSize 归一逻辑只由 request.toMap() 触发一次。
        return page(request, null);
    }

    @Override
    public PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request, RequestOptions options) {
        TaskTemplatePageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // SDK 将平台分页响应统一解包为 PageResult<TaskTemplateInfo>。
        return executor.executeRequiredData(PAGE, requiredRequest,
                new TypeRef<ApiResponse<PageResult<TaskTemplateInfo>>>() {
        }, options);
    }
}
