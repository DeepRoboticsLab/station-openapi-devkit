package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.inventory.DevicePointPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DockListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DogPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MaintainAreaPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.NodeListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.RoadMapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.StreamPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 设备、地图、摄像机与点位等基础数据接口的轻量 Web 入参集合。
 *
 * <p>record 负责 HTTP 结构校验并显式构建 SDK 请求；分页值保持调用方输入，
 * 由 SDK 统一完成默认值、下限和最大页容量归一化。</p>
 */
public final class InventoryInputs {

    private InventoryInputs() {
    }

    public record PageDogs(
            Integer pageNo,
            Integer pageSize,
            String dogName,
            String deviceMode) {

        /**
         * 构建 SDK 请求；分页值不在 Web 层改写，统一交给 SDK 归一化。
         */
        public DogPageRequest toSdkRequest() {
            return DogPageRequest.builder()
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .dogName(dogName)
                    .deviceMode(deviceMode)
                    .build();
        }
    }

    public record Maps(String floor, String dogCode) {

        public MapListRequest toSdkRequest() {
            return MapListRequest.builder()
                    .floor(floor)
                    .dogCode(dogCode)
                    .build();
        }
    }

    public record PageStreams(
            String name,
            String bindDeviceCode,
            String videoMode,
            Integer pageNo,
            Integer pageSize) {

        /**
         * 构建 SDK 请求；分页值不在 Web 层改写，统一交给 SDK 归一化。
         */
        public StreamPageRequest toSdkRequest() {
            return StreamPageRequest.builder()
                    .name(name)
                    .bindDeviceCode(bindDeviceCode)
                    .videoMode(videoMode)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }

    public record PagePoints(
            String pointCode,
            String dogCode,
            Integer pageNo,
            Integer pageSize) {

        /**
         * 构建 SDK 请求；分页值不在 Web 层改写，统一交给 SDK 归一化。
         */
        public DevicePointPageRequest toSdkRequest() {
            return DevicePointPageRequest.builder()
                    .pointCode(pointCode)
                    .dogCode(dogCode)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }

    public record PageMaintainAreas(
            String dogCode,
            String maintainAreaCode,
            String repairDesc,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer issued,
            String floor,
            Integer pageNo,
            Integer pageSize) {

        /**
         * 构建 SDK 请求；分页值不在 Web 层改写，统一交给 SDK 归一化。
         */
        public MaintainAreaPageRequest toSdkRequest() {
            return MaintainAreaPageRequest.builder()
                    .dogCode(dogCode)
                    .maintainAreaCode(maintainAreaCode)
                    .repairDesc(repairDesc)
                    .startTime(startTime)
                    .endTime(endTime)
                    .issued(issued)
                    .floor(floor)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }

    public record RoadMaps(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_ROAD_MAP_DOG_CODE_REQUIRED") String dogCode) {

        public RoadMapListRequest toSdkRequest() {
            return RoadMapListRequest.builder().dogCode(dogCode).build();
        }
    }

    public record Docks(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_DOCK_DOG_CODE_REQUIRED") String dogCode) {

        public DockListRequest toSdkRequest() {
            return DockListRequest.builder().dogCode(dogCode).build();
        }
    }

    public record Nodes(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_NODE_DOG_CODE_REQUIRED") String dogCode) {

        public NodeListRequest toSdkRequest() {
            return NodeListRequest.builder().dogCode(dogCode).build();
        }
    }
}
