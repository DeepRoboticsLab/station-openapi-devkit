package com.deeprobotics.station.openapi.sample.mq;

import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 四类远程消息的最小处理示例，只记录非敏感业务定位字段。
 *
 * <p>生产项目应在这些方法中按业务幂等键执行事务性更新，不要在 Sample 中引入伪数据库或内存幂等缓存。</p>
 * <p>Handler 接收 SDK 已解析的类型安全模型，不记录原始 MQ body，避免日志泄露现场数据。</p>
 */
@Service
public class SampleRemoteMessageHandlers {

    private static final Logger LOG = LoggerFactory.getLogger(SampleRemoteMessageHandlers.class);

    public void handleTaskStatus(TaskStatusMessage message) {
        LOG.info("MQ task status: taskExecuteCode={}, status={}, taskProgress={}",
                message.taskExecuteCode(), message.status(), message.taskProgress());
    }

    public void handleTaskResult(TaskResultMessage message) {
        LOG.info("MQ task result: idempotentKey={}, fileCount={}",
                message.idempotentKey(), message.fileList().size());
    }

    public void handleDogAlarm(DogAlarmMessage message) {
        LOG.info("MQ dog alarm: dogCode={}, alarmCode={}", message.dogCode(), message.alarmCode());
    }

    public void handleRoute(RouteMessage message) {
        LOG.info("MQ route: taskExecuteCode={}, pointCount={}",
                message.taskExecuteCode(), message.deviceCodes().size());
    }
}
