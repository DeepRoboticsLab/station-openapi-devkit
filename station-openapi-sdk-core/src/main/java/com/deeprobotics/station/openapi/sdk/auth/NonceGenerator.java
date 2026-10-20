package com.deeprobotics.station.openapi.sdk.auth;

import java.security.SecureRandom;

/**
 * nonce 生成器。
 *
 * <p>平台通过 nonce 防重放，不允许重复使用。SDK 每次请求和每次重试都必须生成新的 nonce，
 * 因此该接口被设计成可注入，便于测试验证“重试会重新签名”。</p>
 */
@FunctionalInterface
public interface NonceGenerator {

    /**
     * 生成一次性随机串。返回空值会被签名鉴权实现拒绝。
     *
     * @return 本次鉴权使用的非空 nonce
     */
    String generate();

    /**
     * 返回进程内共享的安全随机 nonce 生成器。
     *
     * @return 线程安全的默认生成器
     */
    static NonceGenerator secureRandom() {
        return SecureRandomNonceGenerator.INSTANCE;
    }

    /**
     * 基于 {@link SecureRandom} 的默认 32 位字母数字 nonce 生成器。
     */
    final class SecureRandomNonceGenerator implements NonceGenerator {
        private static final SecureRandomNonceGenerator INSTANCE = new SecureRandomNonceGenerator();
        private static final char[] ALPHABET =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        private static final int NONCE_LENGTH = 32;
        private final SecureRandom random = new SecureRandom();

        private SecureRandomNonceGenerator() {
        }

        @Override
        public String generate() {
            char[] value = new char[NONCE_LENGTH];
            for (int i = 0; i < value.length; i++) {
                value[i] = ALPHABET[random.nextInt(ALPHABET.length)];
            }
            return new String(value);
        }
    }
}
