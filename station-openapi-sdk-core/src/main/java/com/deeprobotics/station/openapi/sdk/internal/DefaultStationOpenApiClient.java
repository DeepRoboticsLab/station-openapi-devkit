package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.api.CameraApi;
import com.deeprobotics.station.openapi.sdk.api.DogApi;
import com.deeprobotics.station.openapi.sdk.api.InventoryApi;
import com.deeprobotics.station.openapi.sdk.api.ResultApi;
import com.deeprobotics.station.openapi.sdk.api.SystemApi;
import com.deeprobotics.station.openapi.sdk.api.TaskApi;
import com.deeprobotics.station.openapi.sdk.api.TaskTemplateApi;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认可执行客户端实现，集中持有七个业务分组和 SDK-owned transport 的生命周期。
 */
public final class DefaultStationOpenApiClient implements StationOpenApiClient {

    private final InventoryApi inventory;
    private final DogApi dog;
    private final CameraApi camera;
    private final TaskTemplateApi taskTemplate;
    private final TaskApi task;
    private final ResultApi result;
    private final SystemApi system;
    private final HttpTransport transport;
    private final boolean transportOwnedBySdk;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 创建聚合七个业务分组的默认客户端。
     *
     * @param executor 所有业务分组共享的统一执行器
     * @param transport 客户端使用的 HTTP transport
     * @param transportOwnedBySdk {@code true} 表示客户端关闭时应一并关闭 transport
     */
    public DefaultStationOpenApiClient(OpenApiExecutor executor, HttpTransport transport,
            boolean transportOwnedBySdk) {
        OpenApiExecutor requiredExecutor = Objects.requireNonNull(executor, "executor is required");
        this.transport = Objects.requireNonNull(transport, "transport is required");
        this.transportOwnedBySdk = transportOwnedBySdk;
        this.inventory = new DefaultInventoryApi(requiredExecutor);
        this.dog = new DefaultDogApi(requiredExecutor);
        this.camera = new DefaultCameraApi(requiredExecutor);
        this.taskTemplate = new DefaultTaskTemplateApi(requiredExecutor);
        this.task = new DefaultTaskApi(requiredExecutor);
        this.result = new DefaultResultApi(requiredExecutor);
        this.system = new DefaultSystemApi(requiredExecutor);
    }

    @Override
    public InventoryApi inventory() {
        return inventory;
    }

    @Override
    public DogApi dog() {
        return dog;
    }

    @Override
    public CameraApi camera() {
        return camera;
    }

    @Override
    public TaskTemplateApi taskTemplate() {
        return taskTemplate;
    }

    @Override
    public TaskApi task() {
        return task;
    }

    @Override
    public ResultApi result() {
        return result;
    }

    @Override
    public SystemApi system() {
        return system;
    }

    @Override
    public void close() {
        /*
         * SDK-owned transport 只关闭一次，允许 Spring 销毁回调、try-with-resources 和显式 close 重复触发。
         * 调用方注入的 transport 不进入 CAS，也不会被 SDK 标记或关闭；它可能仍被应用中的其他组件复用。
         */
        if (transportOwnedBySdk && closed.compareAndSet(false, true)) {
            transport.close();
        }
    }
}
