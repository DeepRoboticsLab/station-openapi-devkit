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
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateStatusRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
import java.util.Objects;

/**
 * 任务模板 API 默认实现。
 *
 * <p>创建、启停和删除是业务写入，不自动重试；详情和分页是只读查询，可按查询策略处理连接类失败。</p>
 */
public final class DefaultTaskTemplateApi implements TaskTemplateApi {

    private static final EndpointDefinition CREATE = EndpointDefinition.of(
            "taskTemplate.create", "/remoteApi/onlySaveTaskModel", OperationIntent.BUSINESS_WRITE);
    private static final EndpointDefinition SWITCH_STATUS = EndpointDefinition.of(
            "taskTemplate.switchStatus", "/remoteApi/switchTaskModelStatus", OperationIntent.BUSINESS_WRITE);
    private static final EndpointDefinition DELETE_BY_CODES = EndpointDefinition.of(
            "taskTemplate.deleteByCodes", "/remoteApi/removeTaskModelByCodes", OperationIntent.BUSINESS_WRITE);
    private static final EndpointDefinition GET_BY_CODE = EndpointDefinition.of(
            "taskTemplate.getByCode", "/remoteApi/taskModelByCode", OperationIntent.READ_ONLY);
    private static final EndpointDefinition PAGE =
            EndpointDefinition.of("taskTemplate.page", "/remoteApi/modelPage", OperationIntent.READ_ONLY);

    private final OpenApiExecutor executor;

    /**
     * 创建任务模板 API 实现。
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultTaskTemplateApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public String create(TaskTemplateCreateRequest request) {
        return create(request, null);
    }

    @Override
    public String create(TaskTemplateCreateRequest request, RequestOptions options) {
        TaskTemplateCreateRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(CREATE, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, options);
    }

    @Override
    public void deleteByCodes(TaskTemplateDeleteRequest request) {
        deleteByCodes(request, null);
    }

    @Override
    public void switchStatus(TaskTemplateStatusRequest request) {
        switchStatus(request, null);
    }

    @Override
    public void switchStatus(TaskTemplateStatusRequest request, RequestOptions options) {
        TaskTemplateStatusRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 启停成功时 data 可为空；启用后仍需另行下发任务。
        executor.execute(SWITCH_STATUS, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public void deleteByCodes(TaskTemplateDeleteRequest request, RequestOptions options) {
        TaskTemplateDeleteRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 删除成功时 data 可为空。
        executor.execute(DELETE_BY_CODES, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public TaskTemplateInfo getByCode(TaskTemplateGetRequest request) {
        return getByCode(request, null);
    }

    @Override
    public TaskTemplateInfo getByCode(TaskTemplateGetRequest request, RequestOptions options) {
        TaskTemplateGetRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(GET_BY_CODE, requiredRequest,
                new TypeRef<ApiResponse<TaskTemplateInfo>>() {
        }, options);
    }

    @Override
    public PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request) {
        return page(request, null);
    }

    @Override
    public PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request, RequestOptions options) {
        TaskTemplatePageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE, requiredRequest,
                new TypeRef<ApiResponse<PageResult<TaskTemplateInfo>>>() {
        }, options);
    }
}
