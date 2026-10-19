package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateStatusRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.taskTemplate()} 分组的模板增删查及启停调用。
 *
 * <p>Sample 不增加 Repository、事务或模板编排，客户可把这些调用嵌入自己的业务 Service。</p>
 */
@Service
public class TaskTemplateSampleService {

    private final StationOpenApiClient client;

    public TaskTemplateSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public String create(TaskTemplateCreateRequest request, RequestOptions options) {
        return client.taskTemplate().create(request, options);
    }

    public void deleteByCodes(TaskTemplateDeleteRequest request, RequestOptions options) {
        client.taskTemplate().deleteByCodes(request, options);
    }

    /**
     * 切换模板状态；启用后是否再次下发由调用方决定。
     *
     * @param request 模板编码和目标状态
     * @param options 本次请求 ID、超时等选项
     */
    public void switchStatus(TaskTemplateStatusRequest request, RequestOptions options) {
        client.taskTemplate().switchStatus(request, options);
    }

    public TaskTemplateInfo getByCode(TaskTemplateGetRequest request, RequestOptions options) {
        return client.taskTemplate().getByCode(request, options);
    }

    public PageResult<TaskTemplateInfo> page(TaskTemplatePageRequest request, RequestOptions options) {
        return client.taskTemplate().page(request, options);
    }
}
