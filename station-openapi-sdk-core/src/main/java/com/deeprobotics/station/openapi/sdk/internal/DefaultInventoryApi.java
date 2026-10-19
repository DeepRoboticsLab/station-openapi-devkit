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
 * <p>本类只负责把 public API 方法映射到平台接口路径，并交给 {@link OpenApiExecutor}
 * 完成请求体构造、鉴权、序列化、HTTP 调用、响应解包、异常转换和重试。它不直接接触具体 HTTP 实现，
 * 以保证 sdk-core 的依赖边界干净。</p>
 */
public final class DefaultInventoryApi implements InventoryApi {

    /**
     * EndpointDefinition 是执行器识别接口行为的唯一元数据来源。
     *
     * <p>{@code OpenApiCoverageTest} 会基于各默认实现中的定义校验 28 个生产接口，因此新增或修改路径时
     * 必须同步评估 OpenAPI 契约，不应只改字符串让覆盖测试通过。</p>
     */
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
        // 显式拒绝 null 请求对象，避免绕过分页默认值、必填字段校验或后续签名源构造。
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
        // 即使当前查询条件都是可选字段，也要求客户传入明确的请求对象，便于后续扩展本地校验。
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
        // 巡检点分页仍走统一 READ_ONLY 执行链路，连接类失败可按查询策略重试并重新生成鉴权信息。
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
        // 维保区域查询包含时间字段，格式化逻辑放在请求模型中，执行器只消费确定性的 wire map。
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
        // dogCode 必填校验由请求模型 toMap() 完成，确保异常发生在本地参数阶段而不是平台业务失败阶段。
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
        // 机巢停靠点依赖 dogCode 过滤，不允许用 null 请求对象生成空 body 调到平台。
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
        // 路网节点和路网边、机巢停靠点保持一致的本地 dogCode 校验策略。
        NodeListRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return immutableList(executor.executeRequiredData(
                LIST_NODES, requiredRequest, new TypeRef<ApiResponse<List<NodeInfo>>>() {
        }, options));
    }

    /**
     * 保留平台响应顺序并冻结列表；不用 {@code List.copyOf}，是为了兼容响应中可能出现的 null 元素。
     */
    private static <T> List<T> immutableList(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
