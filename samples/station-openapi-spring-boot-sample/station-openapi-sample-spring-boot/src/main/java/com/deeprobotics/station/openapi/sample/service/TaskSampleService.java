package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.task.TaskExecuteInfo;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.task()} 分组的任务下发、控制、取消和查询调用。
 *
 * <p>这里不实现任务状态机或业务重试，避免 Sample 演变为生产工作流系统。</p>
 */
@Service
public class TaskSampleService {

    private final StationOpenApiClient client;

    public TaskSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public String issue(TaskIssueRequest request, RequestOptions options) {
        return client.task().issue(request, options);
    }

    public String control(TaskControlRequest request, RequestOptions options) {
        return client.task().control(request, options);
    }

    public void batchCancel(TaskBatchCancelRequest request, RequestOptions options) {
        client.task().batchCancel(request, options);
    }

    public PageResult<TaskExecuteInfo> page(TaskPageRequest request, RequestOptions options) {
        return client.task().page(request, options);
    }
}
