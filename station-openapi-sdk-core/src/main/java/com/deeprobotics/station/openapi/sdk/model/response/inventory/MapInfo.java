package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 地图基础信息。
 *
 * <p>该模型用于调用方加载地图文件、二维地图描述和楼层/设备关系。
 * 文件下载仍应通过 {@code result().downloadResource(...)} 走流式下载接口，不应把路径当成本地文件读取。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MapInfo(
        /**
         * 楼层标识。
         */
        String floor,
        /**
         * 地图关联的四足狗编码。
         */
        String dogCode,
        /**
         * 地图文件名，可作为后续地图文件下载或客户侧展示的业务标识。
         */
        String mapFileName,
        /**
         * 二维地图元信息，按平台字符串透传。
         */
        String twoDMapInfo,
        /**
         * 二维地图图片路径，可作为资源下载接口的 {@code filePath} 参数。
         */
        String twoDMapBmpPath) {
}
