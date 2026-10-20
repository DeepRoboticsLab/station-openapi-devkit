# RocketMQ Message Integration

**Language:** English | [简体中文](mq-events-guide.zh-CN.md)

The SDK supports four message types on the fixed `httpRemote` topic. Spring Boot applications can use RocketMQ Starter
and delegate parsing and dispatch to `RemoteMessageRouter`.

## 1. Prerequisites

Contact your assigned technical support representative to confirm:

- The RocketMQ NameServer address for the target environment;
- The `httpRemote` topic is available;
- Platform remote publishing is enabled with `http_three_part_remote_upload=true`;
- The application is permitted to consume the messages;
- The application host can reach both the NameServer and the Broker addresses and ports returned by it;
- If the platform requires consumer groups to be created in advance, the group name that may be used.

A NameServer normally uses `host:port` format. It is not the Station HTTP endpoint.

## 2. Enable Starter MQ in the Sample

MQ is disabled by default in the main Sample. Set the following properties
in [application.yml](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/resources/application.yml)
using your environment's values:

```yaml
station:
  openapi:
    mq:
      enabled: true
      consumer-group: station-openapi-sample

rocketmq:
  name-server: mq-nameserver.example.com:9876
```

Run `StationOpenApiSampleApplication` in IDEA. Configure the HTTP endpoint and authentication as described in
the [Sample README](../samples/station-openapi-spring-boot-sample/README.md).

After the consumer starts, trigger a new platform message. End-to-end delivery is confirmed only when the corresponding
Handler writes a business summary log.

## 3. Integrate with a Spring Boot Application

See [Source and Dependency Integration](dependency-and-shade.md) for source and dependency preparation. Use these POMs
as the reference Starter dependency combination:

- [Main Sample POM](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/pom.xml)
- [Sample Parent POM](../samples/station-openapi-spring-boot-sample/pom.xml)

Use these examples for configuration, listening, and handling:

- [SampleMqConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/SampleMqConfiguration.java):
  validates required properties and creates the Router;
- [StarterRemoteMessageListener.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/StarterRemoteMessageListener.java):
  receives `MessageExt` and delegates to the Router;
- [SampleRemoteMessageHandlers.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/SampleRemoteMessageHandlers.java):
  handles the four typed SDK message models.

The Listener must not parse JSON itself or log the raw message body. The SDK Router converts each supported Tag into the
corresponding model.

## 4. Supported Messages

| Tag                          | SDK model           | Purpose                            |
|------------------------------|---------------------|------------------------------------|
| `uploadHttpRemoteTaskStatus` | `TaskStatusMessage` | Task status and progress           |
| `uploadHttpRemoteResult`     | `TaskResultMessage` | Inspection point results and files |
| `uploadHttpRemoteAlarm`      | `DogAlarmMessage`   | Quadruped robot alarms             |
| `uploadHttpRemoteRoute`      | `RouteMessage`      | Task inspection route              |

`uploadHttpRemoteDevicePos` is not part of the message scope supported by the SDK. Query real-time position through
`client.dog().getPosition(...)`.

`DogAlarmMessage.alarmTime()` requires platform v1.3.3+. It is a nullable `yyyy-MM-dd HH:mm:ss` string without a time
zone; missing values are not replaced with consumption time. The HTTP alarm field `DogAlarmInfo.alarmTime()` uses
`LocalDateTime`.

`TaskResultMessage.alarmLevel()` is available from platform v1.3.2: `"0"` normal, `"1"` warning, `"2"` general, `"3"`
serious, and `"4"` emergency. The SDK accepts JSON integers and strings and preserves unknown codes. Older messages,
omitted values, and explicit nulls yield `null`, which must not be treated as normal. `confidence()` also remains a
nullable string.

A point may produce a collection result followed by an analysis result that updates confidence and alarm level. Merge
these messages using the business key below.

## 5. Consumer Groups

- Instances of the same application in the same environment use the same group, allowing RocketMQ to distribute messages
  among them.
- Different applications that each need the complete message stream must use different groups.
- The Starter Sample and native MQ Sample must use different groups when run at the same time.
- Do not generate a random group on every startup.

## 6. Message Handling

RocketMQ messages may be delivered more than once. Business handlers must be thread-safe and safely process duplicates:

- Locate a task result by `taskExecuteCode + devicePointCode`;
- Merge file lists by `fileType + filePath`;
- Persist deduplication state if it must survive restarts or work across instances;
- Return normally after successful processing;
- Throw an exception when processing fails so RocketMQ can redeliver the message;
- Log only the Tag and necessary business identifiers, never the raw message body.

The Sample handlers only log summaries. Implement persistence, deduplication, and merging in your application.

## 7. Native Subscriber Sample

For integration without RocketMQ Starter, use `station-openapi-sample-native-mq`.

Set these values in that module's `application.yml`:

```yaml
station:
  openapi:
    native-mq:
      enabled: true
      name-server: mq-nameserver.example.com:9876
      consumer-group: station-openapi-native-sample
```

Then run `NativeMqSampleApplication` in IDEA.

The native module demonstrates MQ consumption only. It does not create an HTTP Client and does not require Token or
signature credentials.
See [NativeMqConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-native-mq/src/main/java/com/deeprobotics/station/openapi/sample/nativemq/NativeMqConfiguration.java)
for lifecycle management and registration of all four handlers.
