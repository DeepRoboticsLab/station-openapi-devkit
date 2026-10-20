/**
 * 可选的 RocketMQ 远程推送订阅模块。
 *
 * <p>模块使用 RocketMQ 原生 client，不依赖 Spring Boot starter。当前 SDK 固定订阅 topic {@code httpRemote}
 * 的四个有效 tag：任务状态、巡检结果、本体告警和巡视路线；坐标通过 HTTP {@code /remoteApi/dogPos} 拉取。</p>
 *
 * <p>未知 tag 或未注册 handler 会 ack-skip；JSON 解析失败和 handler 异常固定请求重投。RocketMQ 可能整批重投，
 * 业务处理器必须使用任务执行编码、巡检点编码等业务键实现幂等处理。</p>
 */
package com.deeprobotics.station.openapi.sdk.rocketmq;
