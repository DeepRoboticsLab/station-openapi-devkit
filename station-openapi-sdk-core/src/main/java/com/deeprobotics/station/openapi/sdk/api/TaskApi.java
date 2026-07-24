package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.task.TaskExecuteInfo;

/**
 * 任务执行 API 分组。
 *
 * <p>任务下发、暂停、恢复、停止和批量取消都属于 TASK_CONTROL，默认不重试。
 * 连接失败或读超时后的实际执行状态可能未知，应通过任务分页、任务状态或 MQ 事件确认。</p>
 *
 * <p>任务分页查询属于 READ_ONLY。控制命令同时提供枚举和 raw code 入口，
 * 既避免调用方记忆数字命令，也可兼容平台新增编码。</p>
 */
public interface TaskApi {

    /**
     * 下发已创建的任务模板并生成一次任务执行记录。
     *
     * <p>对应 {@code POST /remoteApi/issue}。成功后返回平台生成的 {@code taskExecuteCode}，
     * 后续暂停、恢复、终止、批量取消和结果查询都围绕这个执行编码进行。</p>
     *
     * @param request 任务下发请求，必须包含非空 {@code taskCode}
     * @return 本次任务执行编码 {@code taskExecuteCode}；平台成功响应未携带 data 时为 {@code null}
     */
    String issue(TaskIssueRequest request);

    /**
     * 下发任务模板，并为本次调用指定请求级选项。
     *
     * <p>这是 TASK_CONTROL 操作，默认不重试；读超时或连接中断时异常会携带
     * {@code resultUnknown=true}，客户应先查询任务分页或消费 MQ 事件确认是否已经下发。</p>
     *
     * @param request 任务下发请求，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 本次任务执行编码 {@code taskExecuteCode}；平台成功响应未携带 data 时为 {@code null}
     */
    String issue(TaskIssueRequest request, RequestOptions options);

    /**
     * 对任务执行记录下发暂停、恢复或终止指令。
     *
     * <p>对应 {@code POST /remoteApi/taskCtrl}。推荐使用 {@code TaskControlCommand}，避免直接记忆
     * {@code "2"}/{@code "3"}/{@code "4"}；如平台扩展新命令，可通过 raw code 入口透传。</p>
     *
     * @param request 任务控制请求，必须包含非空 {@code taskExecuteCode} 和合法命令
     * @return 平台返回的控制结果字符串；成功响应未携带 {@code data} 时为 {@code null}
     */
    String control(TaskControlRequest request);

    /**
     * 对任务执行记录下发控制指令，并为本次调用指定请求级选项。
     *
     * <p>控制类操作默认不重试，超时后的执行状态需要通过后续查询确认。</p>
     *
     * @param request 任务控制请求，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 平台返回的控制结果字符串；成功响应未携带 {@code data} 时为 {@code null}
     */
    String control(TaskControlRequest request, RequestOptions options);

    /**
     * 批量取消多个任务执行记录。
     *
     * <p>对应 {@code POST /remoteApi/batchCancelTask}。请求体中的 {@code taskExecuteCodeList}
     * 必须是 JSON 数组；任一取消失败时平台会直接返回失败。</p>
     *
     * @param request 批量取消请求，必须包含至少一个任务执行编码
     */
    void batchCancel(TaskBatchCancelRequest request);

    /**
     * 批量取消多个任务执行记录，并为本次调用指定请求级选项。
     *
     * <p>批量取消同样属于 TASK_CONTROL，默认不重试。</p>
     *
     * @param request 批量取消请求，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     */
    void batchCancel(TaskBatchCancelRequest request, RequestOptions options);

    /**
     * 分页查询任务执行记录。
     *
     * <p>对应 {@code POST /remoteApi/taskPage}。这是只读查询，SDK 对外只使用
     * {@code pageNo/pageSize}，不发送历史 {@code pageNum}。</p>
     *
     * @param request 任务执行分页查询条件，日期字段按 {@code yyyy-MM-dd} 发送
     * @return 非 null 的任务执行分页数据
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<TaskExecuteInfo> page(TaskPageRequest request);

    /**
     * 分页查询任务执行记录，并为本次调用指定请求级选项。
     *
     * <p>如果 {@code showUnExecuted} 未设置，请求体会省略该字段，由平台按默认 {@code false} 处理。</p>
     *
     * @param request 任务执行分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的任务执行分页数据
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<TaskExecuteInfo> page(TaskPageRequest request, RequestOptions options);
}
