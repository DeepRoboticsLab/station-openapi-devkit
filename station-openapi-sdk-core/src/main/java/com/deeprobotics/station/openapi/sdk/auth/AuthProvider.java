package com.deeprobotics.station.openapi.sdk.auth;

import java.util.Map;

/**
 * 请求鉴权提供者。
 *
 * <p>executor 将业务请求体交给这里，返回已经补充鉴权信息的 body 和 header。
 * 实现必须复制入参，不能修改原始业务 map；重试时 executor 会重新调用该接口，以生成新的 nonce/sign。</p>
 */
public interface AuthProvider {

    /**
     * 对业务请求体进行鉴权加工。
     *
     * @param businessBody 业务字段，不包含 SDK 运行时 header
     * @return 可直接序列化/发送的鉴权后请求
     */
    AuthenticatedRequest authenticate(Map<String, Object> businessBody);
}
