package com.deeprobotics.station.openapi.sdk.model.response.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务执行记录信息。
 *
 * <p>未知字段会被忽略，以便平台新增展示字段时已有 SDK 仍能解析响应。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskExecuteInfo(
        /** 变电站/站点名称，用于业务系统展示任务所属站点。 */
        String stationName,
        /** 任务模板编码，同一模板多次下发时该值相同。 */
        String taskCode,
        /** 任务名称，通常来自任务模板配置。 */
        String taskName,
        /** 任务执行编码，一次下发或计划实例化生成一个新值，是控制任务和查询结果的核心关联键。 */
        String taskExecuteCode,
        /** 任务优先级，{@code 1} 为最低、{@code 4} 为最高。 */
        Integer priority,
        /** 执行该任务的四足狗编码。 */
        String dogCode,
        /** 任务类型：{@code 1} 独立巡检、{@code 2} 联合巡检、{@code 3} 视频巡检。 */
        Integer taskType,
        /** 执行类型：{@code 1} 立即执行、{@code 2} 定时执行、{@code 3} 周期执行、{@code 4} 间隔执行。 */
        Integer executeType,
        /** 巡检类型编码，例如例行巡视、专项巡视等；历史数据可能存在旧编码，因此保持字符串。 */
        String patrolType,
        /** 任务包含的巡检点编码集合，平台以英文逗号字符串返回。 */
        String pointCodes,
        /** 任务配置的巡检点数量。 */
        Integer pointCount,
        /** 任务进度展示文本，格式由平台生成，调用方宜按展示字符串处理。 */
        String taskProgress,
        /** 计划开始时间。 */
        LocalDateTime planStartTime,
        /** 实际开始时间；任务尚未开始时可能为空。 */
        LocalDateTime startTime,
        /** 实际结束时间；任务未结束时可能为空。 */
        LocalDateTime endTime,
        /** 实际执行点位总数。 */
        Integer allCount,
        /** 正常结果数量。 */
        Integer normalCount,
        /** 告警结果数量。 */
        Integer alarmCount,
        /** 识别正常数量。 */
        Integer recognitionNormalCount,
        /** 识别异常数量。 */
        Integer recognitionErrorCount,
        /** 漏检数量。 */
        Integer missCount,
        /** 巡检路线的保序不可变快照；平台未返回时为空 Map，键值含义由平台接口约定。 */
        Map<String, String> taskRoad,
        /**
         * 任务执行状态：{@code "1"} 已执行、{@code "2"} 正在执行、{@code "3"} 暂停、
         * {@code "4"} 终止、{@code "5"} 未执行、{@code "6"} 超期。
         */
        String status,
        /** 下发状态：{@code 0} 未下发、{@code 1} 已下发、{@code 2} 下发失败。 */
        Integer sendStatus,
        /** 任务结束动作：{@code 1} 自动充电、{@code 2} 原地待命。 */
        Integer endAction) {

    /**
     * 创建任务执行记录，并把巡检路线归一为保序不可变快照。
     *
     * <p>平台未返回 {@code taskRoad} 时使用空 Map；非空 Map 会被复制，避免响应对象受 Jackson
     * 临时集合或调用方后续修改影响。</p>
     *
     * @param stationName 变电站或站点名称
     * @param taskCode 任务模板编码
     * @param taskName 任务名称
     * @param taskExecuteCode 本次任务执行编码
     * @param priority 任务优先级
     * @param dogCode 执行任务的四足狗编码
     * @param taskType 任务类型编码
     * @param executeType 执行类型编码
     * @param patrolType 巡检类型编码
     * @param pointCodes 英文逗号分隔的巡检点编码
     * @param pointCount 任务配置的巡检点数量
     * @param taskProgress 平台生成的任务进度展示文本
     * @param planStartTime 计划开始时间
     * @param startTime 实际开始时间，任务未开始时可为 {@code null}
     * @param endTime 实际结束时间，任务未结束时可为 {@code null}
     * @param allCount 实际执行点位总数
     * @param normalCount 正常结果数量
     * @param alarmCount 告警结果数量
     * @param recognitionNormalCount 识别正常数量
     * @param recognitionErrorCount 识别异常数量
     * @param missCount 漏检数量
     * @param taskRoad 巡检路线；{@code null} 会归一为空 Map，非空值会形成不可变快照
     * @param status 任务执行状态编码
     * @param sendStatus 任务下发状态编码
     * @param endAction 任务结束动作编码
     */
    public TaskExecuteInfo {
        taskRoad = taskRoad == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(taskRoad));
    }
}
