package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.service.InventorySampleService;
import com.deeprobotics.station.openapi.sample.web.dto.InventoryInputs;
import com.deeprobotics.station.openapi.sample.web.view.CameraStreamView;
import com.deeprobotics.station.openapi.sample.web.view.PageView;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DevicePointInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DockInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DogInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.MaintainAreaInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.MapInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.NodeInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.RoadMapInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 台账与基础资源查询的薄 Web 示例，对应 SDK 的 {@code client.inventory()} 分组。
 *
 * <p>Controller 只负责 HTTP 输入输出和 Validation，不创建 Client、不签名，也不解析平台响应包装。</p>
 */
@RestController
@RequestMapping("/sample/api/inventory")
@Tag(name = "Inventory", description = "台账与基础资源查询 / Inventory and basic resources")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class InventorySampleController {

    private final InventorySampleService service;

    public InventorySampleController(InventorySampleService service) {
        this.service = service;
    }

    @PostMapping("/page-dogs")
    @Operation(operationId = "inventory.pageDogs", summary = "分页查询四足狗台账 / Page robot dogs")
    public PageView<DogInfo> pageDogs(
            @Valid @RequestBody InventoryInputs.PageDogs input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pageDogs(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/list-maps")
    @Operation(operationId = "inventory.listMaps", summary = "查询地图列表 / List maps")
    public List<MapInfo> listMaps(
            @Valid @RequestBody InventoryInputs.Maps input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.listMaps(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/page-streams")
    @Operation(operationId = "inventory.pageStreams", summary = "分页查询安全摄像机流视图 / Page safe camera streams")
    public PageView<CameraStreamView> pageStreams(
            @Valid @RequestBody InventoryInputs.PageStreams input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pageStreams(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/page-points")
    @Operation(operationId = "inventory.pagePoints", summary = "分页查询巡检点 / Page inspection points")
    public PageView<DevicePointInfo> pagePoints(
            @Valid @RequestBody InventoryInputs.PagePoints input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pagePoints(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/page-maintain-areas")
    @Operation(operationId = "inventory.pageMaintainAreas", summary = "分页查询维保区域 / Page maintenance areas")
    public PageView<MaintainAreaInfo> pageMaintainAreas(
            @Valid @RequestBody InventoryInputs.PageMaintainAreas input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pageMaintainAreas(
                input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/list-road-maps")
    @Operation(operationId = "inventory.listRoadMaps", summary = "查询路网边 / List road map edges")
    public List<RoadMapInfo> listRoadMaps(
            @Valid @RequestBody InventoryInputs.RoadMaps input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.listRoadMaps(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/list-docks")
    @Operation(operationId = "inventory.listDocks", summary = "查询停靠点 / List dock points")
    public List<DockInfo> listDocks(
            @Valid @RequestBody InventoryInputs.Docks input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.listDocks(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/list-nodes")
    @Operation(operationId = "inventory.listNodes", summary = "查询地图节点 / List map nodes")
    public List<NodeInfo> listNodes(
            @Valid @RequestBody InventoryInputs.Nodes input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.listNodes(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }
}
