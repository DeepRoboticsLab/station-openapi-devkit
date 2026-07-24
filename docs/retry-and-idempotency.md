# Retries and Result Confirmation

**Language:** English | [简体中文](retry-and-idempotency.zh-CN.md)

The SDK prioritizes preventing duplicate commands. Queries and downloads may make at most two attempts for specific network failures. Operations that modify data or control devices make only one attempt.

## 1. Default Attempt Counts

The counts below include the first attempt:

| Operation type | Maximum attempts | Behavior |
|---|---:|---|
| Queries, lists, pages, and status reads | 2 | A second attempt is allowed only for connection failures or timeouts |
| File downloads | 2 | A failed attempt is never returned as a complete file |
| Task issue, control, and cancellation | 1 | The platform may already have received the request |
| Task template creation and deletion | 1 | Platform data may already have changed |
| Device, charging, and camera controls | 1 | A duplicate command may affect field equipment |

HTTP errors, explicit platform business failures, invalid parameters, and response parsing failures are never retried automatically.

`RequestOptions.RetryPolicy.NEVER` can disable the default retry for a query or download, but no per-call option can force retries for write or control operations.

## 2. resultUnknown

If a connection is interrupted or a read times out during a write or control operation, the platform may have received the request even though the caller did not receive the final response. The SDK exception then reports:

```text
resultUnknown=true
```

This value means that the result has not been confirmed; it does not mean success or failure. Handle it as follows:

1. Do not repeat the operation immediately;
2. Record the exception's `requestId`, `operation`, `code`, and occurrence time;
3. Confirm whether the original operation took effect through a query API, a RocketMQ message, or the current field state;
4. Repeat the operation only after confirming that it did not take effect and that the business process permits another attempt.

Typical confirmation paths:

| Original operation | Recommended confirmation |
|---|---|
| Task issue, control, or cancellation | Query task executions or wait for a task status message |
| Task template creation or deletion | Query template details or template pages |
| Quadruped robot control | Query device status and real-time position |
| Camera or PTZ control | Confirm through device state or an authorized field check |

## 3. requestId

`requestId` correlates one HTTP call with its logs and exception. The SDK generates one when the caller does not provide it.

It is not a business idempotency key. Reusing the same `requestId` does not prevent duplicate operations. The customer application must use its own stable business identifiers to determine whether a task, result, or message has already been processed.

## 4. Duplicate RocketMQ Messages

RocketMQ messages may be delivered more than once. Business handlers must safely process the same message repeatedly:

- Use `taskExecuteCode + devicePointCode` as the business key for task results;
- Merge file lists by `fileType + filePath`;
- Throw an exception when processing fails so RocketMQ can redeliver the message;
- Do not rely on an in-memory Set in a single JVM for persistent deduplication;
- Log only the Tag and necessary business identifiers, never the raw message body.

See [RocketMQ Message Integration](mq-events-guide.md) for the four supported message types and configuration.
