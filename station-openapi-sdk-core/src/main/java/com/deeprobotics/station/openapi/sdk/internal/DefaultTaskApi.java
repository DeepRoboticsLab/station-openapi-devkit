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
 * <p>这一层只负责把 {@link TaskApi} 的公共方法绑定到明确的平台 API 路径，并把请求对象交给
 * {@link OpenApiExecutor} 统一完成本地校验、鉴权字段填充、JSON 序列化、HTTP 调用和 {@code R<T>} 解包。
 * 业务字段如何进入 body 由各请求模型的 {@code toMap()} 控制，避免实现类重复拼装字段。</p>
 *
 * <p>任务下发、暂停/恢复/终止、批量取消都会改变机器人或平台任务状态，按设计归类为
 * {@link OperationIntent#TASK_CONTROL}，默认不自动重试；如果网络超时导致异常，调用方需要通过任务分页、
 * 任务状态或 MQ 事件确认真实执行状态。任务分页是 {@link OperationIntent#READ_ONLY}，可按只读查询策略处理。</p>
 */
public final class DefaultTaskApi implements TaskApi {

    /**
     * 按任务模板的执行计划生成并下发任务。
     *
     * <p>立即执行可返回 {@code taskExecuteCode}；定时、周期或间隔计划不保证返回单个编码。
     * 保留平台的可空 data，不把空结果误判为解析失败。</p>
     */
    private static final EndpointDefinition ISSUE =
            EndpointDefinition.of("task.issue", "/remoteApi/issue", OperationIntent.TASK_CONTROL);
    /**
     * 对一次任务执行记录下发控制命令。
     *
     * <p>命令值由 {@code TaskControlCommand} 或 raw code 入口提供，wire 值保持平台要求的字符串编码。</p>
     */
    private static final EndpointDefinition CONTROL =
            EndpointDefinition.of("task.control", "/remoteApi/taskCtrl", OperationIntent.TASK_CONTROL);
    /**
     * 批量取消任务执行记录。
     *
     * <p>请求体中的 {@code taskExecuteCodeList} 必须是 JSON 数组，不能为了签名或兼容性转成逗号字符串。</p>
     */
    private static final EndpointDefinition BATCH_CANCEL =
            EndpointDefinition.of("task.batchCancel", "/remoteApi/batchCancelTask", OperationIntent.TASK_CONTROL);
    /**
     * 任务执行记录分页查询。
     *
     * <p>SDK 公共模型只暴露 {@code pageNo/pageSize}，不会发送平台历史兼容字段 {@code pageNum}。</p>
     */
    private static final EndpointDefinition PAGE =
            EndpointDefinition.of("task.page", "/remoteApi/taskPage", OperationIntent.READ_ONLY);

    /**
     * OpenAPI 执行器，集中封装鉴权、重试、响应解析和异常归一。
     *
     * <p>实现类保持很薄，便于后续审查某个公共方法是否绑定到了正确 endpoint 和 operation intent。</p>
     */
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
        /*
         * execute() 会调用 request.toMap() 生成签名源和 JSON body，并把 R<String>.data 作为返回值。
         * 因为 ISSUE 是 TASK_CONTROL，默认重试策略会避免在超时场景下重复下发任务。
         */
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
        /*
         * 控制接口返回平台 String data。这里不把 data 解释成枚举或布尔值，避免平台调整文案时破坏兼容性。
         */
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
        /*
         * 平台只通过统一响应表示成功或失败，SDK 公共方法返回 void；如果任一任务取消失败，
         * execute() 会按统一 R<T> 规则抛出业务异常。
         */
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
        /*
         * PageResult 对 current/size/pages 等字段做了宽松兼容；任务分页当前返回完整分页字段，
         * 但 SDK 仍通过统一分页模型承接，方便客户按同一方式处理各类分页接口。
         */
        return executor.executeRequiredData(PAGE, requiredRequest,
                new TypeRef<ApiResponse<PageResult<TaskExecuteInfo>>>() {
        }, options);
    }
}
