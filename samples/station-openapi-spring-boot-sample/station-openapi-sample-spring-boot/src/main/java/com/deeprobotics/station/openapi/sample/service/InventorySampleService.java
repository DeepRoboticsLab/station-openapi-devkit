package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sample.web.view.CameraStreamView;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
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
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.inventory()} 分组的台账查询方式。
 *
 * <p>摄像机流通过 {@link CameraStreamView} 返回，剔除密码并过滤不适合展示的流地址。</p>
 */
@Service
public class InventorySampleService {

    private final StationOpenApiClient client;

    public InventorySampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public PageResult<DogInfo> pageDogs(DogPageRequest request, RequestOptions options) {
        return client.inventory().pageDogs(request, options);
    }

    public List<MapInfo> listMaps(MapListRequest request, RequestOptions options) {
        return client.inventory().listMaps(request, options);
    }

    public PageResult<CameraStreamView> pageStreams(StreamPageRequest request, RequestOptions options) {
        PageResult<CameraStreamInfo> page = client.inventory().pageStreams(request, options);
        // SDK 模型包含摄像机密码；Sample 转换为安全 View，避免 JSON 与 Swagger 暴露密码。
        List<CameraStreamView> records = page.records().stream()
                .map(CameraStreamView::from)
                .toList();
        return new PageResult<>(records, page.total(), page.pageNo(), page.pageSize(), page.pages());
    }

    public PageResult<DevicePointInfo> pagePoints(DevicePointPageRequest request, RequestOptions options) {
        return client.inventory().pagePoints(request, options);
    }

    public PageResult<MaintainAreaInfo> pageMaintainAreas(
            MaintainAreaPageRequest request, RequestOptions options) {
        return client.inventory().pageMaintainAreas(request, options);
    }

    public List<RoadMapInfo> listRoadMaps(RoadMapListRequest request, RequestOptions options) {
        return client.inventory().listRoadMaps(request, options);
    }

    public List<DockInfo> listDocks(DockListRequest request, RequestOptions options) {
        return client.inventory().listDocks(request, options);
    }

    public List<NodeInfo> listNodes(NodeListRequest request, RequestOptions options) {
        return client.inventory().listNodes(request, options);
    }
}
