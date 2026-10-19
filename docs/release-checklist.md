# Integration Checklist

**Language:** English | [简体中文](release-checklist.zh-CN.md)

Use this checklist after configuring the SDK or Sample to confirm the basic integration.

## SDK and Application

- [ ] The application uses JDK 17 or later.
- [ ] Spring Boot uses version 3.x.
- [ ] The SDK source is copied into the project, or the complete SDK modules are available from a private Nexus.
- [ ] Source integration includes the OkHttp transport `META-INF/services` SPI file.
- [ ] The source or private artifacts use version `1.0.0-SNAPSHOT`.

## Endpoint and Credentials

- [ ] Technical support supplied an endpoint beginning with `http://` or `https://`.
- [ ] No individual `/remoteApi/*` path is appended to the endpoint.
- [ ] Technical support supplied either an Access Token or an AppKey and SecretKey pair.
- [ ] Exactly one of Token and signature authentication is enabled.
- [ ] Real credentials are not committed to a public repository or written to logs.

## First Calls

- [ ] Spring creates only one `StationOpenApiClient` bean.
- [ ] The Client calls `close()` when the application stops.
- [ ] `client.system().getTimeText()` succeeds.
- [ ] At least one Inventory query succeeds.
- [ ] Pagination requests use `pageNo`.
- [ ] Exception logs contain only necessary fields such as `requestId`, `operation`, `code`, and `resultUnknown`.

## Optional Capabilities

- [ ] File downloads use `Path` or `OutputStream` and never load the complete file into a `byte[]`.
- [ ] A write or control failure with `resultUnknown=true` is confirmed before any repeat attempt.
- [ ] The Sample's eleven dangerous operations remain locked by default or are enabled only after confirming the target.
- [ ] When MQ is required, technical support has supplied the NameServer and confirmed the topic and message permissions.
- [ ] MQ handlers tolerate duplicate messages and never log the raw message body.
