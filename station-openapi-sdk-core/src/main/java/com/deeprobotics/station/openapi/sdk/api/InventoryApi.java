package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
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
import java.util.List;

/**
 * 台账与基础资源查询 API 分组。
 *
 * <p>该分组覆盖与现场基础资料相关的只读接口：狗台账、地图、摄像头流、巡检点、
 * 维保区域、路网边、机巢停靠点和路网节点。所有方法都通过 {@code POST /remoteApi/*}
 * 访问平台，底层操作意图统一为 {@code READ_ONLY}，因此 SDK 默认可以在连接类失败时按查询接口策略重试。</p>
 *
 * <p>分页请求必须使用 {@code pageNo/pageSize}。平台可能兼容历史页码字段，
 * 但 SDK 公共 API 和生产请求体都不得重新引入该历史字段，避免请求签名源和文档示例产生歧义。</p>
 *
 * <p>每个业务方法都提供普通重载和 {@link RequestOptions} 重载。普通重载适合大多数调用；
 * 带 options 的重载用于单次覆盖 requestId、超时或重试策略，方便调用方在排查现场问题时串联日志。</p>
 */
public interface InventoryApi {

    /**
     * 分页查询四足狗台账。
     *
     * <p>对应 {@code POST /remoteApi/dogPage}。请求体只发送 {@code pageNo/pageSize}
     * 以及可选的 {@code dogName/deviceMode} 过滤条件；分页默认值和上限由请求模型统一归一化。</p>
     *
     * @param request 四足狗分页查询条件，不能为 {@code null}
     * @return 非 null 的四足狗台账分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DogInfo> pageDogs(DogPageRequest request);

    /**
     * 分页查询四足狗台账，并为本次调用指定请求级选项。
     *
     * @param request 四足狗分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的四足狗台账分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DogInfo> pageDogs(DogPageRequest request, RequestOptions options);

    /**
     * 查询地图列表。
     *
     * <p>对应 {@code POST /remoteApi/mapList}。{@code floor} 和 {@code dogCode}
     * 都是可选过滤条件，未传时由平台按当前租户或授权范围返回可见地图。</p>
     *
     * @param request 地图查询条件，不能为 {@code null}
     * @return 非 null 的保序不可变地图列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<MapInfo> listMaps(MapListRequest request);

    /**
     * 查询地图列表，并为本次调用指定请求级选项。
     *
     * @param request 地图查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的保序不可变地图列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<MapInfo> listMaps(MapListRequest request, RequestOptions options);

    /**
     * 分页查询摄像机流媒体配置。
     *
     * <p>对应 {@code POST /remoteApi/streamPage}。该接口的分页响应只保证包含
     * {@code records/total}，因此返回的 {@link PageResult#pageNo()}、
     * {@link PageResult#pageSize()} 和 {@link PageResult#pages()} 可能为 {@code null}。</p>
     *
     * @param request 摄像机流分页查询条件，不能为 {@code null}
     * @return 非 null 的摄像机流媒体分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<CameraStreamInfo> pageStreams(StreamPageRequest request);

    /**
     * 分页查询摄像机流媒体配置，并为本次调用指定请求级选项。
     *
     * @param request 摄像机流分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的摄像机流媒体分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<CameraStreamInfo> pageStreams(StreamPageRequest request, RequestOptions options);

    /**
     * 分页查询巡检点基础信息。
     *
     * <p>对应 {@code POST /remoteApi/pointPage}。平台会过滤充电点等非巡检点类型，
     * SDK 保持字段透传，不在客户端重复做业务筛选。</p>
     *
     * @param request 巡检点分页查询条件，不能为 {@code null}
     * @return 非 null 的巡检点分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DevicePointInfo> pagePoints(DevicePointPageRequest request);

    /**
     * 分页查询巡检点基础信息，并为本次调用指定请求级选项。
     *
     * @param request 巡检点分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的巡检点分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DevicePointInfo> pagePoints(DevicePointPageRequest request, RequestOptions options);

    /**
     * 分页查询维保区域。
     *
     * <p>对应 {@code POST /remoteApi/maintainAreaPage}。时间过滤条件由请求模型格式化为
     * 平台要求的 {@code yyyy-MM-dd HH:mm:ss} 字符串，避免 Java time 默认格式影响签名和查询。</p>
     *
     * @param request 维保区域分页查询条件，不能为 {@code null}
     * @return 非 null 的维保区域分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<MaintainAreaInfo> pageMaintainAreas(MaintainAreaPageRequest request);

    /**
     * 分页查询维保区域，并为本次调用指定请求级选项。
     *
     * @param request 维保区域分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的维保区域分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<MaintainAreaInfo> pageMaintainAreas(MaintainAreaPageRequest request, RequestOptions options);

    /**
     * 查询指定狗的路网边列表。
     *
     * <p>对应 {@code POST /remoteApi/roadMapList}。{@code dogCode} 是平台必填字段，
     * SDK 在请求模型中提前做本地校验，避免发送无效请求。</p>
     *
     * @param request 路网边查询条件，必须包含非空 {@code dogCode}
     * @return 非 null 的保序不可变路网边列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<RoadMapInfo> listRoadMaps(RoadMapListRequest request);

    /**
     * 查询指定狗的路网边列表，并为本次调用指定请求级选项。
     *
     * @param request 路网边查询条件，必须包含非空 {@code dogCode}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的保序不可变路网边列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<RoadMapInfo> listRoadMaps(RoadMapListRequest request, RequestOptions options);

    /**
     * 查询指定狗的机巢停靠点列表。
     *
     * <p>对应 {@code POST /remoteApi/dockList}。{@code dogCode} 是平台必填字段，
     * SDK 在请求模型中提前做本地校验。</p>
     *
     * @param request 机巢停靠点查询条件，必须包含非空 {@code dogCode}
     * @return 非 null 的保序不可变机巢停靠点列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<DockInfo> listDocks(DockListRequest request);

    /**
     * 查询指定狗的机巢停靠点列表，并为本次调用指定请求级选项。
     *
     * @param request 机巢停靠点查询条件，必须包含非空 {@code dogCode}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的保序不可变机巢停靠点列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<DockInfo> listDocks(DockListRequest request, RequestOptions options);

    /**
     * 查询指定狗的路网节点列表。
     *
     * <p>对应 {@code POST /remoteApi/nodeList}。{@code dogCode} 是平台必填字段，
     * SDK 在请求模型中提前做本地校验。</p>
     *
     * @param request 路网节点查询条件，必须包含非空 {@code dogCode}
     * @return 非 null 的保序不可变路网节点列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<NodeInfo> listNodes(NodeListRequest request);

    /**
     * 查询指定狗的路网节点列表，并为本次调用指定请求级选项。
     *
     * @param request 路网节点查询条件，必须包含非空 {@code dogCode}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的保序不可变路网节点列表
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    List<NodeInfo> listNodes(NodeListRequest request, RequestOptions options);
}
