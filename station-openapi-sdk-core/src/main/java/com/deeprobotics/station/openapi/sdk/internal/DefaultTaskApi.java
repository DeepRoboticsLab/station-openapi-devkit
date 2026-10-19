package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.TaskApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.task.TaskExecuteInfo;
import java.util.Objects;

/**
 * 任务执行 API 默认实现。
 *
 * <p>将业务方法映射到平台路径，统一由 {@link OpenApiExecutor} 执行。</p>
 *
 * <p>下发、控制和取消任务不自动重试；断连后应查询任务或等待 MQ 确认状态。分页按只读策略执行。</p>
 */
public final class DefaultTaskApi implements TaskApi {

    private static final EndpointDefinition ISSUE =
            EndpointDefinition.of("task.issue", "/remoteApi/issue", OperationIntent.TASK_CONTROL);
    private static final EndpointDefinition CONTROL =
            EndpointDefinition.of("task.control", "/remoteApi/taskCtrl", OperationIntent.TASK_CONTROL);
    private static final EndpointDefinition BATCH_CANCEL =
            EndpointDefinition.of("task.batchCancel", "/remoteApi/batchCancelTask", OperationIntent.TASK_CONTROL);
    private static final EndpointDefinition PAGE =
            EndpointDefinition.of("task.page", "/remoteApi/taskPage", OperationIntent.READ_ONLY);

    private final OpenApiExecutor executor;

    /**
     * 创建任务执行 API 实现。
     *
     * @param executor 已完成客户端级配置的执行器，不能为 {@code null}
     */
    public DefaultTaskApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public String issue(TaskIssueRequest request) {
        return issue(request, null);
    }

    @Override
    public String issue(TaskIssueRequest request, RequestOptions options) {
        TaskIssueRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 立即执行可返回 taskExecuteCode；定时、周期或间隔计划的 data 可为空。
        return executor.execute(ISSUE, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, options);
    }

    @Override
    public String control(TaskControlRequest request) {
        return control(request, null);
    }

    @Override
    public String control(TaskControlRequest request, RequestOptions options) {
        TaskControlRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 保留平台可空的字符串结果，不推断为枚举或布尔值。
        return executor.execute(CONTROL, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, options);
    }

    @Override
    public void batchCancel(TaskBatchCancelRequest request) {
        batchCancel(request, null);
    }

    @Override
    public void batchCancel(TaskBatchCancelRequest request, RequestOptions options) {
        TaskBatchCancelRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(BATCH_CANCEL, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public PageResult<TaskExecuteInfo> page(TaskPageRequest request) {
        return page(request, null);
    }

    @Override
    public PageResult<TaskExecuteInfo> page(TaskPageRequest request, RequestOptions options) {
        TaskPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE, requiredRequest,
                new TypeRef<ApiResponse<PageResult<TaskExecuteInfo>>>() {
        }, options);
    }
}
