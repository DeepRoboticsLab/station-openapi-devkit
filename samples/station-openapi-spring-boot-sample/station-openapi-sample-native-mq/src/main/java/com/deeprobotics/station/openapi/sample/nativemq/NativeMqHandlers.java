package com.deeprobotics.station.openapi.sample.nativemq;

import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 原生 Subscriber 的四类最小 Handler，只输出非敏感业务定位字段。
 *
 * <p>这里接收与 Starter 示例相同的四类 SDK 模型，不记录原始 MQ body；生产项目应按业务幂等键
 * 在事务中持久化消息，而不是照搬日志示例。</p>
 */
@Service
public class NativeMqHandlers {

    private static final Logger LOG = LoggerFactory.getLogger(NativeMqHandlers.class);

    public void handleTaskStatus(TaskStatusMessage message) {
        LOG.info("Native MQ task status: taskExecuteCode={}, status={}, taskProgress={}",
                message.taskExecuteCode(), message.status(), message.taskProgress());
    }

    /**
     * 展示结果摘要；业务处理应按幂等键更新结果并合并文件，缺失告警等级不能视为正常。
     *
     * @param message SDK 已解析的结果消息，alarmLevel 允许为 null
     */
    public void handleTaskResult(TaskResultMessage message) {
        LOG.info("Native MQ task result: idempotentKey={}, fileCount={}, alarmLevel={}",
                message.idempotentKey(), message.fileList().size(), message.alarmLevel());
    }

    /**
     * 展示平台记录的告警发生时间，旧消息缺失时不补当前时间。
     *
     * @param message SDK 告警消息，alarmTime 是不携带时区的本地时间文本，允许为 null
     */
    public void handleDogAlarm(DogAlarmMessage message) {
        LOG.info("Native MQ dog alarm: dogCode={}, alarmCode={}, alarmTime={}",
                message.dogCode(), message.alarmCode(), message.alarmTime());
    }

    public void handleRoute(RouteMessage message) {
        LOG.info("Native MQ route: taskExecuteCode={}, pointCount={}",
                message.taskExecuteCode(), message.deviceCodes().size());
    }
}
