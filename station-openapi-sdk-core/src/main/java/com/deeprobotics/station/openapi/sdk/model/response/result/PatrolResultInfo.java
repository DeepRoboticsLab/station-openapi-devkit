package com.deeprobotics.station.openapi.sdk.model.response.result;

import com.deeprobotics.station.openapi.sdk.model.common.FileInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 巡检点结果记录。
 *
 * <p>表示平台返回的巡检点结果。文件列表中的
 * {@code filePath} 可直接作为资源下载接口入参。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatrolResultInfo(
        /** 区域编码，表示巡检点所属的业务区域。 */
        String areaCode,
        /** 区域名称，用于业务系统展示。 */
        String areaName,
        /** 设备所属间隔的编码。 */
        String intervalCode,
        /** 间隔名称。 */
        String intervalName,
        /** 主设备编码。 */
        String deviceCode,
        /** 主设备名称。 */
        String deviceName,
        /** 部件编码；没有部件层级的数据可能为空。 */
        String componentCode,
        /** 部件名称。 */
        String componentName,
        /** 巡检点编码，是定位某个具体点位结果的主要业务键。 */
        String devicePointCode,
        /** 巡检点名称。 */
        String devicePointName,
        /** 任务模板编码。 */
        String taskCode,
        /** 任务名称。 */
        String taskName,
        /** 任务执行编码，可与任务分页结果中的 taskExecuteCode 对应。 */
        String taskExecuteCode,
        /** 识别类型编码，例如表计、状态、红外等，具体字典以平台配置为准。 */
        String recognitionType,
        /** 设备类型编码，SDK 按字符串透传。 */
        String deviceType,
        /** 三相标识，例如 A/B/C 或平台配置的相位编码。 */
        String phase,
        /** 三相分组 ID，用于把同一设备的多相点位关联展示。 */
        String phaseGroupId,
        /** 执行巡检的四足狗编码。 */
        String dogCode,
        /** 巡检采集或识别时间。 */
        LocalDateTime patrolTime,
        /** 平台处理后的巡检值，统一以字符串返回，调用方可按识别类型自行转换。 */
        String patrolValue,
        /** 巡检值单位，例如摄氏度、百分比或平台字典单位。 */
        String patrolUnit,
        /**
         * 巡检文件资源列表，{@code filePath} 可直接作为资源下载接口入参。
         *
         * <p>平台当前常见文件类型为：1 红外图谱、2 可见光照片、3 音频；SDK 保留原始类型字符串，
         * 以兼容后续扩展。</p>
         */
        List<FileInfo> fileList,
        /** 告警等级：0 正常、1 警告、2 一般、3 严重、4 紧急；null 表示未提供，不能视为正常。 */
        Integer alarmLevel,
        /** 告警类型编码。 */
        String alarmType,
        /** 告警描述文本。 */
        String alarmContent,
        /** 识别结论编码：{@code 1} 正确/有效，{@code 0} 采集失败，{@code 2} 分析失败。 */
        Integer valid,
        /** 原始识别值，用于保留算法或设备侧未格式化前的结果。 */
        String originalValue,
        /** 算法置信度，通常为 0~1 之间的数值字符串，也可能为空。 */
        String confidence) {

    /**
     * 创建巡检点结果，并把缺失文件列表归一为空的不可变列表。
     *
     * @param areaCode 区域编码
     * @param areaName 区域名称
     * @param intervalCode 间隔编码
     * @param intervalName 间隔名称
     * @param deviceCode 主设备编码
     * @param deviceName 主设备名称
     * @param componentCode 部件编码
     * @param componentName 部件名称
     * @param devicePointCode 巡检点编码
     * @param devicePointName 巡检点名称
     * @param taskCode 任务模板编码
     * @param taskName 任务名称
     * @param taskExecuteCode 任务执行编码
     * @param recognitionType 识别类型编码
     * @param deviceType 设备类型编码
     * @param phase 三相标识
     * @param phaseGroupId 三相分组 ID
     * @param dogCode 四足狗编码
     * @param patrolTime 巡检采集或识别时间
     * @param patrolValue 巡检值
     * @param patrolUnit 巡检值单位
     * @param fileList 巡检文件列表，允许为 {@code null}
     * @param alarmLevel 告警等级
     * @param alarmType 告警类型编码
     * @param alarmContent 告警描述
     * @param valid 识别结论编码
     * @param originalValue 原始识别值
     * @param confidence 算法置信度，通常为 0~1 之间的数值字符串
     */
    public PatrolResultInfo {
        /*
         * 对调用方暴露不可变列表，避免调用方修改反序列化结果后影响后续业务判断。
         * 平台未返回 fileList 时统一为空列表，调用方遍历时无需额外做 null 判断。
         */
        fileList = fileList == null ? List.of() : List.copyOf(fileList);
    }
}
