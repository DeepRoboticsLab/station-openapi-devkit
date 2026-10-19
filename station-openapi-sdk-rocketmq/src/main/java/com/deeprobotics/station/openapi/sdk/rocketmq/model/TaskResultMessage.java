package com.deeprobotics.station.openapi.sdk.rocketmq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 巡检结果推送消息。
 *
 * <p>对应 {@code uploadHttpRemoteResult} tag。平台在识别流程中可能对同一个
 * {@code taskExecuteCode + devicePointCode} 先推原图结果，再推原图+效果图结果；客户侧应使用
 * {@link #idempotentKey()} 做 upsert 或文件列表合并，而不是简单插入两条独立记录。</p>
 *
 * <p>{@code alarmLevel} 从平台 v1.3.2 起提供，SDK 将整数或字符串统一保留为可空字符串。
 * 缺失等级和置信度均不补默认值；未知字段仍忽略，以兼容不同平台版本。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskResultMessage(
        /** 执行巡检的四足狗编码。 */
        String dogCode,
        /** 执行巡检的四足狗名称。 */
        String dogName,
        /** 任务名称，便于客户在 MQ 消息中直接展示上下文。 */
        String taskName,
        /** 任务模板编码。 */
        String taskCode,
        /** 巡检点名称。 */
        String devicePointName,
        /** 巡检点编码，与 taskExecuteCode 共同组成推荐幂等键。 */
        String devicePointCode,
        /** 巡检值，按平台处理后的字符串返回。 */
        String patrolValue,
        /** 巡检值单位，例如摄氏度、百分比或平台字典单位。 */
        String patrolUnit,
        /** 巡检时间，按平台字符串透传。 */
        String patrolTime,
        /** 识别类型编码，具体含义以平台配置为准。 */
        String recognitionType,
        /** 巡检文件列表。filePath 可用于 /remoteApi/result/resource 下载。 */
        List<FileInfo> fileList,
        /** 任务执行编码，是一次实际任务执行的唯一标识。 */
        String taskExecuteCode,
        /** 识别结论编码，例如 0 采集失败、1 正确、2 分析失败；SDK 按字符串透传。 */
        String valid,
        /** 算法置信度，通常为 0~1 之间的数值字符串；采集阶段平台默认上送 {@code "1"}。 */
        String confidence,
        /** 当前任务状态编码，SDK 按字符串透传。 */
        String status,
        /** 计划开始时间，平台格式为 {@code yyyy-MM-dd HH:mm:ss}。 */
        String planStartTime,
        /** 实际开始时间。 */
        String startTime,
        /** 当前任务进度。 */
        String taskProgress,
        /** 预计剩余时间。 */
        String remainTime,
        /** 平台返回的任务描述或执行说明。 */
        String description,
        /**
         * 告警等级，平台 v1.3.2 起提供：0 正常、1 警告、2 一般、3 严重、4 紧急。
         * 数字和字符串均映射为 String；旧平台或缺失值保留 null，不等同于正常。
         */
        String alarmLevel) {

    /**
     * 把缺失的文件列表归一为空的不可变列表。
     *
     * @param dogCode 四足狗编码
     * @param dogName 四足狗名称
     * @param taskName 任务名称
     * @param taskCode 任务模板编码
     * @param devicePointName 巡检点名称
     * @param devicePointCode 巡检点编码
     * @param patrolValue 巡检值
     * @param patrolUnit 巡检值单位
     * @param patrolTime 巡检时间文本
     * @param recognitionType 识别类型编码
     * @param fileList 巡检文件列表，允许为 {@code null}
     * @param taskExecuteCode 任务执行编码
     * @param valid 识别结论编码
     * @param confidence 算法置信度
     * @param status 当前任务状态编码
     * @param planStartTime 计划开始时间
     * @param startTime 实际开始时间
     * @param taskProgress 当前任务进度
     * @param remainTime 预计剩余时间
     * @param description 任务描述或执行说明
     * @param alarmLevel 告警等级，缺失时为 null
     */
    public TaskResultMessage {
        fileList = fileList == null ? List.of() : List.copyOf(fileList);
    }

    /**
     * 兼容未包含告警等级时的原有构造器。
     *
     * <p>保留旧调用方的源代码和构造器签名；告警等级保持未知，不填充正常值。</p>
     *
     * @param dogCode 四足狗编码
     * @param dogName 四足狗名称
     * @param taskName 任务名称
     * @param taskCode 任务模板编码
     * @param devicePointName 巡检点名称
     * @param devicePointCode 巡检点编码
     * @param patrolValue 巡检值
     * @param patrolUnit 巡检单位
     * @param patrolTime 巡检时间文本
     * @param recognitionType 识别类型
     * @param fileList 文件列表，null 归一为空列表
     * @param taskExecuteCode 任务执行编码
     * @param valid 识别结论
     * @param confidence 算法置信度
     * @param status 当前任务状态
     * @param planStartTime 计划开始时间
     * @param startTime 实际开始时间
     * @param taskProgress 任务进度
     * @param remainTime 预计剩余时间
     * @param description 任务描述
     */
    public TaskResultMessage(String dogCode, String dogName, String taskName, String taskCode,
            String devicePointName, String devicePointCode, String patrolValue, String patrolUnit,
            String patrolTime, String recognitionType, List<FileInfo> fileList, String taskExecuteCode,
            String valid, String confidence, String status, String planStartTime, String startTime,
            String taskProgress, String remainTime, String description) {
        this(dogCode, dogName, taskName, taskCode, devicePointName, devicePointCode, patrolValue, patrolUnit,
                patrolTime, recognitionType, fileList, taskExecuteCode, valid, confidence, status,
                planStartTime, startTime, taskProgress, remainTime, description, null);
    }

    /**
     * 巡检结果推荐幂等键。
     *
     * <p>同一点位结果可能因为效果图生成而二次推送，客户侧可用该键做 upsert，并对 fileList 做并集。</p>
     *
     * @return {@code taskExecuteCode + ":" + devicePointCode}
     */
    public String idempotentKey() {
        return taskExecuteCode + ":" + devicePointCode;
    }

    /**
     * 巡检结果文件。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileInfo(
            /** 文件类型编码：1 红外图谱、2 可见光照片、3 音频、4 视频、5 效果图、101 声纹图、102 效果视频。 */
            String fileType,
            /** 文件路径，可作为资源下载接口的 filePath 入参。 */
            String filePath) {
    }
}
