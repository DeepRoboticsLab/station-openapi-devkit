package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.InventoryApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DevicePointPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DockListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DogPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MaintainAreaPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.NodeListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.RoadMapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.StreamPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DevicePointInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DockInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DogInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.MaintainAreaInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.MapInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.NodeInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.RoadMapInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 台账与基础资源查询 API 默认实现。
 *
 * <p>将业务方法映射到平台路径，统一由 {@link OpenApiExecutor} 执行。</p>
 */
public final class DefaultInventoryApi implements InventoryApi {

    private static final EndpointDefinition PAGE_DOGS =
            EndpointDefinition.of("inventory.pageDogs", "/remoteApi/dogPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition LIST_MAPS =
            EndpointDefinition.of("inventory.listMaps", "/remoteApi/mapList", OperationIntent.READ_ONLY);
    private static final EndpointDefinition PAGE_STREAMS =
            EndpointDefinition.of("inventory.pageStreams", "/remoteApi/streamPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition PAGE_POINTS =
            EndpointDefinition.of("inventory.pagePoints", "/remoteApi/pointPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition PAGE_MAINTAIN_AREAS = EndpointDefinition.of(
            "inventory.pageMaintainAreas", "/remoteApi/maintainAreaPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition LIST_ROAD_MAPS =
            EndpointDefinition.of("inventory.listRoadMaps", "/remoteApi/roadMapList", OperationIntent.READ_ONLY);
    private static final EndpointDefinition LIST_DOCKS =
            EndpointDefinition.of("inventory.listDocks", "/remoteApi/dockList", OperationIntent.READ_ONLY);
    private static final EndpointDefinition LIST_NODES =
            EndpointDefinition.of("inventory.listNodes", "/remoteApi/nodeList", OperationIntent.READ_ONLY);

    private final OpenApiExecutor executor;

    /**
     * 创建台账与基础资源 API 实现。
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultInventoryApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public PageResult<DogInfo> pageDogs(DogPageRequest request) {
        return pageDogs(request, null);
    }

    @Override
    public PageResult<DogInfo> pageDogs(DogPageRequest request, RequestOptions options) {
        DogPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_DOGS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<DogInfo>>>() {
        }, options);
    }

    @Override
    public List<MapInfo> listMaps(MapListRequest request) {
        return listMaps(request, null);
    }

    @Override
    public List<MapInfo> listMaps(MapListRequest request, RequestOptions options) {
        MapListRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return immutableList(executor.executeRequiredData(
                LIST_MAPS, requiredRequest, new TypeRef<ApiResponse<List<MapInfo>>>() {
        }, options));
    }

    @Override
    public PageResult<CameraStreamInfo> pageStreams(StreamPageRequest request) {
        return pageStreams(request, null);
    }

    @Override
    public PageResult<CameraStreamInfo> pageStreams(StreamPageRequest request, RequestOptions options) {
        // /streamPage 响应可能省略 current/size/pages，由通用分页模型兼容。
        StreamPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_STREAMS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<CameraStreamInfo>>>() {
        }, options);
    }

    @Override
    public PageResult<DevicePointInfo> pagePoints(DevicePointPageRequest request) {
        return pagePoints(request, null);
    }

    @Override
    public PageResult<DevicePointInfo> pagePoints(DevicePointPageRequest request, RequestOptions options) {
        DevicePointPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_POINTS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<DevicePointInfo>>>() {
        }, options);
    }

    @Override
    public PageResult<MaintainAreaInfo> pageMaintainAreas(MaintainAreaPageRequest request) {
        return pageMaintainAreas(request, null);
    }

    @Override
    public PageResult<MaintainAreaInfo> pageMaintainAreas(MaintainAreaPageRequest request, RequestOptions options) {
        MaintainAreaPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_MAINTAIN_AREAS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<MaintainAreaInfo>>>() {
        }, options);
    }

    @Override
    public List<RoadMapInfo> listRoadMaps(RoadMapListRequest request) {
        return listRoadMaps(request, null);
    }

    @Override
    public List<RoadMapInfo> listRoadMaps(RoadMapListRequest request, RequestOptions options) {
        RoadMapListRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return immutableList(executor.executeRequiredData(LIST_ROAD_MAPS, requiredRequest,
                new TypeRef<ApiResponse<List<RoadMapInfo>>>() {
        }, options));
    }

    @Override
    public List<DockInfo> listDocks(DockListRequest request) {
        return listDocks(request, null);
    }

    @Override
    public List<DockInfo> listDocks(DockListRequest request, RequestOptions options) {
        DockListRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return immutableList(executor.executeRequiredData(
                LIST_DOCKS, requiredRequest, new TypeRef<ApiResponse<List<DockInfo>>>() {
        }, options));
    }

    @Override
    public List<NodeInfo> listNodes(NodeListRequest request) {
        return listNodes(request, null);
    }

    @Override
    public List<NodeInfo> listNodes(NodeListRequest request, RequestOptions options) {
        NodeListRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return immutableList(executor.executeRequiredData(
                LIST_NODES, requiredRequest, new TypeRef<ApiResponse<List<NodeInfo>>>() {
        }, options));
    }

    /**
     * 返回只读副本，保留响应顺序和 null 元素（{@code List.copyOf} 不接受 null）。
     */
    private static <T> List<T> immutableList(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
