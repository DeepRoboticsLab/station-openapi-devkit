/**
 * 按业务域组织的 OpenAPI 请求模型。
 *
 * <p>builder 负责收集调用方输入，必填校验、格式化和分页归一通常在 {@code toMap()} 执行，确保首次请求与重试
 * 使用同一套规则。{@code taskCodes/taskExecuteCodeList} 必须保持 JSON 数组；
 * {@code pointCodes/cycleMonth/cycleWeek/statusArr} 必须按平台 String 字段输出英文逗号字符串。</p>
 */
package com.deeprobotics.station.openapi.sdk.model.request;
