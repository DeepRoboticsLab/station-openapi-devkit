/**
 * Station OpenAPI SDK 的客户入口与客户端级配置。
 *
 * <p>典型调用链为：</p>
 * <ol>
 *     <li>通过 {@link com.deeprobotics.station.openapi.sdk.StationOpenApiClients#builder()} 配置 endpoint 和一种鉴权方式；</li>
 *     <li>builder 通过 JDK SPI 自动发现默认 HTTP transport，或使用客户显式注入的 transport；</li>
 *     <li>客户端按业务域暴露 inventory、dog、camera、taskTemplate、task、result、system 七个 API 分组；</li>
 *     <li>所有请求最终进入 core 执行器，统一完成鉴权、序列化、重试、响应解包和异常转换。</li>
 * </ol>
 *
 * <p>默认 provider 创建的 transport 生命周期归 SDK，客户端关闭时一并释放；客户显式注入的 transport
 * 始终由客户管理。客户端适合以单例或应用级 Bean 复用，不应为每次业务调用重复创建。</p>
 */
package com.deeprobotics.station.openapi.sdk;
