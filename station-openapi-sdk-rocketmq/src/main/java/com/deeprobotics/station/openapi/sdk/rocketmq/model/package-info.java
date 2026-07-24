/**
 * 四类远程推送消息的客户侧模型。
 *
 * <p>模型忽略未知字段，并把容易出现数字/字符串差异的外部编码保留为 String。巡检结果可能对同一
 * {@code taskExecuteCode + devicePointCode} 推送多次，调用方应使用模型提供的幂等键做 upsert 和文件并集。</p>
 */
package com.deeprobotics.station.openapi.sdk.rocketmq.model;
