/**
 * 按业务域组织的响应模型。
 *
 * <p>这些类型独立建模平台 JSON wire contract。枚举和状态字段通常保留 String/Integer
 * 原值，未知字段被忽略以支持平台向前扩展。所有对外返回的集合都会在响应模型构造器或 API 返回边界
 * 形成不可变快照；契约允许缺失的集合会归一为空集合。</p>
 */
package com.deeprobotics.station.openapi.sdk.model.response;
