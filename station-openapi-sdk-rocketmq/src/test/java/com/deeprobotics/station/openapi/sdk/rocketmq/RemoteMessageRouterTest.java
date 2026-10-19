package com.deeprobotics.station.openapi.sdk.rocketmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * RocketMQ topic/tag 覆盖、消息解析和路由结果测试。
 *
 * <p>锁定且仅锁定四个 tag、坐标/未知 tag 确认后跳过、未注册 handler 确认后跳过、handler 失败重投，
 * 并同时覆盖普通 JSON 对象与外层字符串包裹的 JSON。测试不启动真实 broker。</p>
 */
class RemoteMessageRouterTest {

    @Test
    void taskResultPreservesNumericAndStringAlarmLevelsIncludingUnknownCodes() {
        AtomicReference<TaskResultMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder().onTaskResult(captured::set).build();
        for (String value : List.of("0", "4", "\"3\"", "\"99\"")) {
            String body = "{\"taskExecuteCode\":\"E-1\",\"devicePointCode\":\"P-1\",\"alarmLevel\":" + value + "}";
            assertThat(router.route(RemoteMessageTags.TAG_TASK_RESULT, bytes(body)))
                    .isEqualTo(RemoteMessageConsumeResult.ACK);
            assertThat(captured.get().alarmLevel()).isEqualTo(value.replace("\"", ""));
        }
    }

    @Test
    void historicalTaskResultsKeepMissingAlarmLevelUnknown() {
        AtomicReference<TaskResultMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder().onTaskResult(captured::set).build();
        for (String body : List.of("{}", "{\"alarmLevel\":null}")) {
            assertThat(router.route(RemoteMessageTags.TAG_TASK_RESULT, bytes(body)))
                    .isEqualTo(RemoteMessageConsumeResult.ACK);
            assertThat(captured.get().alarmLevel()).isNull();
            assertThat(captured.get().confidence()).isNull();
            assertThat(captured.get().fileList()).isEmpty();
        }
        TaskResultMessage legacy = new TaskResultMessage(null, null, null, null, null, "P-1",
                null, null, null, null, null, "E-1", null, null, null, null, null, null, null, null);
        assertThat(legacy.alarmLevel()).isNull();
        assertThat(legacy.idempotentKey()).isEqualTo("E-1:P-1");
        assertThat(legacy.fileList()).isEmpty();
    }

    @Test
    void exposesHttpRemoteTopicAndExactlyFourSupportedTags() {
        assertThat(RemoteMessageTags.TOPIC_HTTP_REMOTE).isEqualTo("httpRemote");
        assertThat(RemoteMessageTags.supportedTags())
                .containsExactlyInAnyOrder(
                        "uploadHttpRemoteTaskStatus",
                        "uploadHttpRemoteResult",
                        "uploadHttpRemoteAlarm",
                        "uploadHttpRemoteRoute")
                .doesNotContain("uploadHttpRemoteDevicePos");
        assertThat(RemoteMessageTags.subscriptionExpression())
                .isEqualTo("uploadHttpRemoteTaskStatus||uploadHttpRemoteResult"
                        + "||uploadHttpRemoteAlarm||uploadHttpRemoteRoute");
        assertThat(RemoteMessageTags.subscriptionExpression().split(Pattern.quote("||")))
                .containsExactlyInAnyOrder(
                        "uploadHttpRemoteTaskStatus",
                        "uploadHttpRemoteResult",
                        "uploadHttpRemoteAlarm",
                        "uploadHttpRemoteRoute");
    }

    @Test
    void routesTaskStatusTagToTaskStatusMessage() {
        AtomicReference<TaskStatusMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskStatus(captured::set)
                .build();

        RemoteMessageConsumeResult result = router.route(RemoteMessageTags.TAG_TASK_STATUS, bytes("""
                {
                  "taskExecuteCode":"exec-001",
                  "taskCode":"task-001",
                  "taskName":"全面巡检",
                  "status":2,
                  "planStartTime":"2026-01-31 16:05:55",
                  "startTime":"2026-01-31 16:06:23",
                  "taskProgress":"45.00",
                  "remainTime":"10m",
                  "description":"running",
                  "futureField":"ignored"
                }
                """));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().taskExecuteCode()).isEqualTo("exec-001");
        assertThat(captured.get().taskCode()).isEqualTo("task-001");
        assertThat(captured.get().status()).isEqualTo("2");
        assertThat(captured.get().taskProgress()).isEqualTo("45.00");
    }

    @Test
    void routesTaskResultObjectJsonToTaskResultMessage() {
        AtomicReference<TaskResultMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskResult(captured::set)
                .build();

        RemoteMessageConsumeResult result = router.route(RemoteMessageTags.TAG_TASK_RESULT, bytes("""
                {
                  "dogCode":"DOG-001",
                  "dogName":"X30-1",
                  "taskExecuteCode":"exec-001",
                  "taskCode":"task-001",
                  "taskName":"全面巡检",
                  "devicePointCode":"point-001",
                  "devicePointName":"开关柜A",
                  "patrolValue":"36.5",
                  "patrolUnit":"C",
                  "patrolTime":"2026-01-31 16:10:00",
                  "recognitionType":"meter",
                  "confidence":"0.86",
                  "valid":1,
                  "status":2,
                  "fileList":[
                    {"fileType":2,"filePath":"/result/original.jpg","unknown":"ignored"}
                  ],
                  "futureField":{"nested":true}
                }
                """));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().dogCode()).isEqualTo("DOG-001");
        assertThat(captured.get().confidence()).isEqualTo("0.86");
        assertThat(captured.get().valid()).isEqualTo("1");
        assertThat(captured.get().status()).isEqualTo("2");
        assertThat(captured.get().fileList()).singleElement().satisfies(file -> {
            assertThat(file.fileType()).isEqualTo("2");
            assertThat(file.filePath()).isEqualTo("/result/original.jpg");
        });
        assertThat(captured.get().idempotentKey()).isEqualTo("exec-001:point-001");
    }

    @Test
    void routesStringWrappedTaskResultJsonToTaskResultMessage() {
        AtomicReference<TaskResultMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskResult(captured::set)
                .build();

        String wrapped = "\"{\\\"taskExecuteCode\\\":\\\"exec-002\\\","
                + "\\\"devicePointCode\\\":\\\"point-002\\\","
                + "\\\"fileList\\\":[{\\\"fileType\\\":\\\"5\\\","
                + "\\\"filePath\\\":\\\"/result/effect.jpg\\\"}]}\"";

        RemoteMessageConsumeResult result = router.route(RemoteMessageTags.TAG_TASK_RESULT, bytes(wrapped));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().idempotentKey()).isEqualTo("exec-002:point-002");
        assertThat(captured.get().fileList()).singleElement()
                .extracting(TaskResultMessage.FileInfo::fileType)
                .isEqualTo("5");
    }

    @Test
    void routesDogAlarmTagToDogAlarmMessage() {
        AtomicReference<DogAlarmMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onDogAlarm(captured::set)
                .build();

        RemoteMessageConsumeResult result = router.route(RemoteMessageTags.TAG_DOG_ALARM, bytes("""
                {
                  "dogCode":"DOG-001",
                  "dogName":"X30-1",
                  "alarmCode":90003,
                  "alarmContent":"机器人远离路网"
                }
                """));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().alarmCode()).isEqualTo("90003");
        assertThat(captured.get().alarmContent()).isEqualTo("机器人远离路网");
    }

    @Test
    void routesRouteTagToRouteMessage() {
        AtomicReference<RouteMessage> captured = new AtomicReference<>();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onRoute(captured::set)
                .build();

        RemoteMessageConsumeResult result = router.route(RemoteMessageTags.TAG_ROUTE, bytes("""
                {
                  "dogCode":"DOG-001",
                  "dogName":"X30-1",
                  "taskExecuteCode":"exec-003",
                  "taskPath":["1,2,3,4","5,6,7,8"],
                  "deviceCodes":["P-001","P-002"]
                }
                """));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().taskPath()).containsExactly("1,2,3,4", "5,6,7,8");
        assertThat(captured.get().deviceCodes()).containsExactly("P-001", "P-002");
    }

    @Test
    void devicePositionAndUnknownTagsAreAckSkippedWithoutRouting() {
        AtomicInteger handled = new AtomicInteger();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskStatus(message -> handled.incrementAndGet())
                .onTaskResult(message -> handled.incrementAndGet())
                .onDogAlarm(message -> handled.incrementAndGet())
                .onRoute(message -> handled.incrementAndGet())
                .build();

        assertThat(router.route(RemoteMessageTags.TAG_DEVICE_POS, bytes("{\"dogCode\":\"DOG-001\"}")))
                .isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(router.route("unknownTag", bytes("{\"anything\":true}")))
                .isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(handled).hasValue(0);
    }

    @Test
    void nullTagIsAckSkippedWithoutCallingHandler() {
        AtomicInteger handled = new AtomicInteger();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskStatus(message -> handled.incrementAndGet())
                .build();

        assertThat(RemoteMessageTags.isSupportedTag(null)).isFalse();
        assertThat(router.route(null, bytes("{\"taskExecuteCode\":\"exec-001\"}")))
                .isEqualTo(RemoteMessageConsumeResult.ACK);
        assertThat(handled).hasValue(0);
    }

    @Test
    void jsonNullAndMalformedJsonAreRetriedWithoutCallingHandler() {
        AtomicInteger handled = new AtomicInteger();
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskStatus(message -> handled.incrementAndGet())
                .build();

        for (String payload : new String[] {"null", "\"null\"", "{bad-json"}) {
            assertThat(router.route(RemoteMessageTags.TAG_TASK_STATUS, bytes(payload)))
                    .as("invalid MQ payload: %s", payload)
                    .isEqualTo(RemoteMessageConsumeResult.RECONSUME_LATER);
        }
        assertThat(handled).hasValue(0);
    }

    @Test
    void interruptedHandlerRequestsRetryAndRestoresInterruptFlag() {
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskStatus(message -> {
                    throw new InterruptedException("shutdown requested");
                })
                .build();

        try {
            RemoteMessageConsumeResult result = router.route(
                    RemoteMessageTags.TAG_TASK_STATUS,
                    bytes("{\"taskExecuteCode\":\"exec-001\"}"));

            assertThat(result).isEqualTo(RemoteMessageConsumeResult.RECONSUME_LATER);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void mqBuildersUseModuleExceptionAndNormalizeConnectionNames() throws Exception {
        assertThatThrownBy(() -> RemoteMessageSubscriber.builder()
                .consumerGroup("group-a")
                .build())
                .isExactlyInstanceOf(RemoteMessageException.class)
                .hasMessageContaining("nameServer");
        assertThatThrownBy(() -> RemoteMessageSubscriber.builder()
                .nameServer("127.0.0.1:9876")
                .consumerGroup(" ")
                .build())
                .isExactlyInstanceOf(RemoteMessageException.class)
                .hasMessageContaining("consumerGroup");
        assertThatThrownBy(() -> RemoteMessageRouter.builder().jsonCodec(null))
                .isExactlyInstanceOf(RemoteMessageException.class)
                .hasMessageContaining("jsonCodec");

        RemoteMessageSubscriber subscriber = RemoteMessageSubscriber.builder()
                .nameServer(" 127.0.0.1:9876 ")
                .consumerGroup(" customer_station_openapi ")
                .build();

        assertThat(readField(subscriber, "nameServer")).isEqualTo("127.0.0.1:9876");
        assertThat(readField(subscriber, "consumerGroup")).isEqualTo("customer_station_openapi");
        subscriber.close();
        subscriber.close();
    }

    @Test
    void knownTagWithoutHandlerIsAckSkipped() {
        RemoteMessageRouter router = RemoteMessageRouter.builder().build();

        assertThat(router.route(RemoteMessageTags.TAG_TASK_STATUS, bytes("{\"taskExecuteCode\":\"exec-001\"}")))
                .isEqualTo(RemoteMessageConsumeResult.ACK);
    }

    @Test
    void handlerExceptionSurfacesAsConsumeFailure() {
        RemoteMessageRouter router = RemoteMessageRouter.builder()
                .onTaskResult(message -> {
                    throw new IllegalStateException("customer handler failed");
                })
                .build();

        RemoteMessageConsumeResult result = router.route(
                RemoteMessageTags.TAG_TASK_RESULT,
                bytes("{\"taskExecuteCode\":\"exec-001\",\"devicePointCode\":\"point-001\"}"));

        assertThat(result).isEqualTo(RemoteMessageConsumeResult.RECONSUME_LATER);
    }

    @Test
    void subscriberBuilderUsesNativeTopicAndSupportedTagExpression() {
        RemoteMessageSubscriber subscriber = RemoteMessageSubscriber.builder()
                .nameServer("127.0.0.1:9876")
                .consumerGroup("customer_station_openapi")
                .onTaskStatus(message -> {
                })
                .onTaskResult(message -> {
                })
                .onDogAlarm(message -> {
                })
                .onRoute(message -> {
                })
                .build();

        assertThat(subscriber.topic()).isEqualTo(RemoteMessageTags.TOPIC_HTTP_REMOTE);
        assertThat(subscriber.tagExpression()).isEqualTo(RemoteMessageTags.subscriptionExpression());
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
