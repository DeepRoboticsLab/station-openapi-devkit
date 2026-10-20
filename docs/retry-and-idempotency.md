# Retries and Result Confirmation

**Language:** English | [简体中文](retry-and-idempotency.zh-CN.md)

The SDK retries queries only after connection failures or timeouts. Downloads, writes, and controls are never retried
automatically.

## 1. Default Attempt Counts

The counts below include the first attempt:

| Operation type                                       | Maximum attempts | Behavior                                                             |
|------------------------------------------------------|-----------------:|----------------------------------------------------------------------|
| Queries, lists, pages, and status reads              |                2 | A second attempt is allowed only for connection failures or timeouts |
| File downloads                                       |                1 | Bytes go directly to the caller; failure may leave partial output    |
| Task issue, control, and cancellation                |                1 | The platform may already have received the request                   |
| Task template creation, status changes, and deletion |                1 | Platform data may already have changed                               |
| Device, charging, and camera controls                |                1 | A duplicate command may affect field equipment                       |

HTTP errors, explicit platform business failures, invalid parameters, and response parsing failures are never retried
automatically.

`RequestOptions.RetryPolicy.NEVER` disables query retries. No per-call option can enable retries for downloads, writes,
or controls.

## 2. resultUnknown

If a connection is interrupted or a read times out during a write or control operation, the platform may have received
the request even though the caller did not receive the final response. The SDK exception then reports:

```text
resultUnknown=true
```

This value means that the result has not been confirmed; it does not mean success or failure. Handle it as follows:

1. Do not repeat the operation immediately;
2. Record the exception's `requestId`, `operation`, `code`, and occurrence time;
3. Confirm whether the original operation took effect through a query API, a RocketMQ message, or the current field
   state;
4. Repeat the operation only after confirming that it did not take effect and that the business process permits another
   attempt.

Typical confirmation paths:

| Original operation                                 | Recommended confirmation                                  |
|----------------------------------------------------|-----------------------------------------------------------|
| Task issue, control, or cancellation               | Query task executions or wait for a task status message   |
| Task template creation, status change, or deletion | Query template details or template pages                  |
| Quadruped robot control                            | Query device status and real-time position                |
| Camera or PTZ control                              | Confirm through device state or an authorized field check |

Batch cancellation runs in list order and stops at the first failure. Completed cancellations are not rolled back, so
query individual states even after an explicit business failure.

Disabling a template removes pending tasks; enabling does not issue tasks automatically. After an unknown status-change
result, check the template detail `isEnable` field and related executions.

Download connection failures, timeouts, and output failures also report `resultUnknown=true`. Discard incomplete output
before restarting; see [File Downloads](file-download-guide.md).

## 3. requestId

The SDK sends `requestId` in `X-Request-Id` and includes it in call failures. It generates one if omitted and reuses it
across query retries.

It is not a business idempotency key. Reusing the same `requestId` does not prevent duplicate operations. The customer
application must use its own stable business identifiers to determine whether a task, result, or message has already
been processed.

For MQ deduplication and result merging, see [RocketMQ Message Integration](mq-events-guide.md).
