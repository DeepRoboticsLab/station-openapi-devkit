package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 签名模式鉴权实现。
 *
 * <p>请求体携带 {@code appKey/timestamp/nonce/sign}；secretKey 仅用于本地签名，不进入请求、日志或异常。</p>
 *
 * <p>每次 {@link #authenticate(Map)} 重新生成 timestamp/nonce/sign，重试时须重新调用。</p>
 */
public final class SignatureAuthProvider implements AuthProvider {

    private final Credential credential;
    private final SdkClock clock;
    private final NonceGenerator nonceGenerator;

    /**
     * 使用系统时间和安全随机 nonce 创建签名 provider。
     *
     * @param credential 必须是包含 appKey/secretKey 的签名凭证
     */
    public SignatureAuthProvider(Credential credential) {
        this(credential, SdkClock.system(), NonceGenerator.secureRandom());
    }

    /**
     * 使用指定时间源和 nonce 生成器创建签名 provider，主要用于确定性测试或受控运行环境。
     *
     * @param credential 签名模式凭证
     * @param clock timestamp 时间源
     * @param nonceGenerator 每次鉴权调用使用的一次性随机串生成器
     */
    public SignatureAuthProvider(Credential credential, SdkClock clock, NonceGenerator nonceGenerator) {
        this.credential = requireCredential(credential);
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.nonceGenerator = Objects.requireNonNull(nonceGenerator, "nonceGenerator is required");
    }

    @Override
    public AuthenticatedRequest authenticate(Map<String, Object> businessBody) {
        // 在副本中清除旧鉴权字段，保留调用方原始数据。
        LinkedHashMap<String, Object> body = MapUtils.copyWithoutNulls(businessBody);
        removeAuthFields(body);

        body.put("appKey", credential.appKey());
        body.put("timestamp", String.valueOf(clock.currentTimeMillis()));
        body.put("nonce", requireText(nonceGenerator.generate(), "nonce"));
        // 鉴权字段就绪后再计算签名。
        body.put("sign", sign(body, credential.secretKey()));
        return new AuthenticatedRequest(body, new LinkedHashMap<>());
    }

    /**
     * 按平台签名规则计算签名。
     *
     * <p>排序使用 Java String 自然序；null 和空白字符串不参与签名；集合使用 Java List.toString()
     * 形态参与拼接；最终对 UTF-8 字节做 MD5 并转大写十六进制。</p>
     */
    static String sign(Map<String, Object> requestBody, String secretKey) {
        // 签名副本会临时加入 secretKey，但这份 map 不会被序列化发送。
        LinkedHashMap<String, Object> signingMap = MapUtils.copyWithoutNulls(requestBody);
        signingMap.put("secretKey", requireText(secretKey, "secretKey"));
        signingMap.remove("sign");

        String source = signingMap.entrySet().stream()
                .filter(entry -> shouldSign(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return md5Upper(source);
    }

    private static Credential requireCredential(Credential credential) {
        if (credential == null) {
            throw new StationOpenApiAuthException("signature credential is required");
        }
        if (credential.authMode() != AuthMode.SIGNATURE) {
            throw new StationOpenApiAuthException("signature credential is required");
        }
        return credential;
    }

    private static void removeAuthFields(Map<String, Object> body) {
        body.remove("appKey");
        body.remove("timestamp");
        body.remove("nonce");
        body.remove("sign");
        body.remove("secretKey");
    }

    private static boolean shouldSign(Object value) {
        if (value == null) {
            return false;
        }
        // 与平台 Hutool StrUtil.isBlankIfStr 的字符集合保持一致；trim/isBlank 均不等价。
        // 仅影响签名参与字段，不改业务 body，也不剥除非空字符串首尾字符。
        return !(value instanceof String text && text.chars().allMatch(character ->
                Character.isWhitespace(character) || Character.isSpaceChar(character)
                        || character == '\ufeff' || character == '\u202a' || character == 0));
    }

    private static String md5Upper(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 digest is not available", ex);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiAuthException(fieldName + " is required");
        }
        return value.trim();
    }
}
