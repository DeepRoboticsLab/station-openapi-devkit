package com.deeprobotics.station.openapi.sdk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 签名与 Token 鉴权契约测试。
 *
 * <p>通过固定 ASCII/中文签名向量锁定 Java 字典序、UTF-8、MD5 大写、空白值跳过和 List 字符串形态；
 * 同时验证每次调用重新生成 nonce/sign、原业务集合不可反向修改鉴权快照，以及 secretKey/token
 * 永不进入 body 或字符串输出。</p>
 */
class AuthProviderTest {

    private static final String SECRET_KEY = "kQwIOrYvnXmSDkwEiFngrKidMcdrgKor";

    @Test
    void signingSkipsTheSameBlankStringsAsPlatformWithoutChangingBusinessBody() {
        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("app", "synthetic-key"), fixedClock("1000"), fixedNonce("nonce"));
        // 平台 Hutool 5.7.20 会跳过这些空白值；签名向量不含 dogName，业务 JSON 仍保留原值。
        for (String blank : List.of("", " \t\r\n", "\u00a0", "\u202f", "\u3000", "\ufeff", "\u202a", "\0")) {
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("dogName", blank);
            AuthenticatedRequest request = provider.authenticate(body);
            assertThat(request.body().get("sign")).isEqualTo("092E362BD812EDEDE5886DF91A24A27E");
            assertThat(request.body().get("dogName")).isEqualTo(blank);
        }
    }

    @Test
    void signingPreservesNonBlankControlCharactersAndMixedWhitespace() {
        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("app", "synthetic-key"), fixedClock("1000"), fixedNonce("nonce"));
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("dogName", "\u0001");
        assertThat(provider.authenticate(body).body().get("sign"))
                .isEqualTo("B4D68C86AA9AC2C5A96A8163305281D9");
        body.put("dogName", "A\u3000");
        assertThat(provider.authenticate(body).body().get("sign"))
                .isEqualTo("6CCE89CC24DEAC07113893E5C94AE5D7");
    }

    @Test
    void stationOpenApiConfigNestedAuthModeCanBridgeToAuthPackageMode() {
        assertThat(StationOpenApiConfig.AuthMode.SIGNATURE.toAuthMode()).isEqualTo(AuthMode.SIGNATURE);
        assertThat(StationOpenApiConfig.AuthMode.ACCESS_TOKEN.toAuthMode()).isEqualTo(AuthMode.ACCESS_TOKEN);
    }

    @Test
    void signatureAuthProviderAddsCommonFieldsAndDoesNotLeakSecretKeyOrMutateBusinessBody() {
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("dogCode", "001");
        business.put("type", 2);
        business.put("command", 4);
        business.put("value", "0.6");

        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                fixedNonce("kwnHHRwBTRqRaQpt9Krbp2vsjUN3aNvV"));

        AuthenticatedRequest request = provider.authenticate(business);

        assertThat(request.body())
                .containsEntry("dogCode", "001")
                .containsEntry("appKey", "123")
                .containsEntry("timestamp", "1770283714727")
                .containsEntry("nonce", "kwnHHRwBTRqRaQpt9Krbp2vsjUN3aNvV")
                .containsEntry("sign", "4B288C90527FF99B11D364E698744201")
                .doesNotContainKey("secretKey");
        assertThat(business)
                .containsOnlyKeys("dogCode", "type", "command", "value")
                .doesNotContainKeys("appKey", "timestamp", "nonce", "sign", "secretKey");
    }

    @Test
    void signatureAuthProviderUsesUtf8AndJavaStringOrderingForChineseVector() {
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("dogName", "测试");
        business.put("deviceMode", "X30");
        business.put("pageNo", 1);
        business.put("pageSize", 15);

        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770601227007"),
                fixedNonce("HypPCn0tMJW549OMPNu32A3fyLSbsQ3w"));

        AuthenticatedRequest request = provider.authenticate(business);

        assertThat(request.body())
                .containsEntry("sign", "239F11C5D380CABA806A5792ED7D4B07")
                .doesNotContainKey("secretKey");
    }

    @Test
    void accessTokenAuthProviderAddsTokenHeaderWithoutSignatureBodyFields() {
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("dogCode", "001");

        AccessTokenAuthProvider provider = new AccessTokenAuthProvider(Credential.accessToken("token-value"));

        AuthenticatedRequest request = provider.authenticate(business);

        assertThat(request.headers()).containsEntry("X-Access-Token", "token-value");
        assertThat(request.body())
                .containsEntry("dogCode", "001")
                .doesNotContainKeys("appKey", "timestamp", "nonce", "sign", "secretKey");
        assertThat(business).containsOnlyKeys("dogCode");
    }

    @Test
    void tokenAuthenticatedRequestToStringRedactsFullTokenHeaderValue() {
        AccessTokenAuthProvider provider = new AccessTokenAuthProvider(Credential.accessToken("token-value"));

        AuthenticatedRequest request = provider.authenticate(new LinkedHashMap<>());

        assertThat(request.toString())
                .contains("X-Access-Token")
                .contains("<redacted>")
                .doesNotContain("token-value");
    }

    @Test
    void tokenModeWithEmptyBusinessBodySendsEmptyJsonObjectWithTokenHeader() {
        AccessTokenAuthProvider provider = new AccessTokenAuthProvider(Credential.accessToken("token-value"));

        AuthenticatedRequest request = provider.authenticate(new LinkedHashMap<>());

        assertThat(request.body()).isEmpty();
        assertThat(request.headers()).containsEntry("X-Access-Token", "token-value");
    }

    @Test
    void signatureAuthProviderRegeneratesNonceAndSignForEachCallIncludingRetry() {
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("dogCode", "001");

        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                sequenceNonce("nonce-A", "nonce-B"));

        AuthenticatedRequest first = provider.authenticate(business);
        AuthenticatedRequest retry = provider.authenticate(business);

        assertThat(first.body().get("nonce")).isEqualTo("nonce-A");
        assertThat(retry.body().get("nonce")).isEqualTo("nonce-B");
        assertThat(first.body().get("sign")).isNotEqualTo(retry.body().get("sign"));
    }

    @Test
    void mutatingOriginalListAfterSignatureAuthenticationDoesNotChangeBodyOrSign() {
        List<String> taskCodes = new ArrayList<>(List.of("T001", "T002"));
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("taskCodes", taskCodes);

        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                fixedNonce("list-nonce"));
        AuthenticatedRequest request = provider.authenticate(business);
        String originalSign = (String) request.body().get("sign");

        taskCodes.add("T003");

        assertThat(request.body().get("taskCodes")).isEqualTo(List.of("T001", "T002"));
        assertThat(request.body().get("sign")).isEqualTo(originalSign);
    }

    @Test
    void authenticatedRequestBodyDoesNotExposeMutableInternalCollectionState() {
        List<String> taskCodes = new ArrayList<>(List.of("T001", "T002"));
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("taskCodes", taskCodes);

        AuthenticatedRequest request = new AccessTokenAuthProvider(Credential.accessToken("token-value"))
                .authenticate(business);
        @SuppressWarnings("unchecked")
        List<String> returnedTaskCodes = (List<String>) request.body().get("taskCodes");

        returnedTaskCodes.add("T003");

        assertThat(request.body().get("taskCodes")).isEqualTo(List.of("T001", "T002"));
    }

    @Test
    void blankStringsRemainInBodyButAreSkippedInSignSource() {
        LinkedHashMap<String, Object> withBlankStrings = new LinkedHashMap<>();
        withBlankStrings.put("dogCode", "001");
        withBlankStrings.put("empty", "");
        withBlankStrings.put("blank", "   ");

        LinkedHashMap<String, Object> withoutBlankStrings = new LinkedHashMap<>();
        withoutBlankStrings.put("dogCode", "001");

        SignatureAuthProvider providerWithBlanks = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                fixedNonce("same-nonce"));
        SignatureAuthProvider providerWithoutBlanks = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                fixedNonce("same-nonce"));

        AuthenticatedRequest requestWithBlanks = providerWithBlanks.authenticate(withBlankStrings);
        AuthenticatedRequest requestWithoutBlanks = providerWithoutBlanks.authenticate(withoutBlankStrings);

        assertThat(requestWithBlanks.body())
                .containsEntry("empty", "")
                .containsEntry("blank", "   ");
        assertThat(requestWithBlanks.body().get("sign")).isEqualTo(requestWithoutBlanks.body().get("sign"));
    }

    @Test
    void listValuesUseJavaListToStringShapeWhenSigning() {
        LinkedHashMap<String, Object> business = new LinkedHashMap<>();
        business.put("taskCodes", List.of("T001", "T002"));

        SignatureAuthProvider provider = new SignatureAuthProvider(
                Credential.signature("123", SECRET_KEY),
                fixedClock("1770283714727"),
                fixedNonce("list-nonce"));

        AuthenticatedRequest request = provider.authenticate(business);

        String source = "appKey=123&nonce=list-nonce&secretKey=" + SECRET_KEY
                + "&taskCodes=[T001, T002]&timestamp=1770283714727";
        assertThat(request.body().get("sign")).isEqualTo(md5Upper(source));
    }

    @Test
    void credentialAndAuthenticatedRequestToStringRedactSensitiveValues() {
        Credential signature = Credential.signature("123", SECRET_KEY);
        Credential token = Credential.accessToken("token-value");
        SignatureAuthProvider provider = new SignatureAuthProvider(
                signature,
                fixedClock("1770283714727"),
                fixedNonce("list-nonce"));
        AuthenticatedRequest request = provider.authenticate(new LinkedHashMap<>());

        assertThat(signature.toString()).doesNotContain(SECRET_KEY);
        assertThat(token.toString()).doesNotContain("token-value");
        assertThat(request.toString())
                .doesNotContain(SECRET_KEY)
                .doesNotContain((CharSequence) request.body().get("sign"));
    }

    @Test
    void blankCredentialsThrowSdkClientExceptionWithRedactedMessages() {
        assertThatThrownBy(() -> Credential.signature(" ", SECRET_KEY))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("appKey")
                .satisfies(thrown -> assertThat(thrown.getMessage()).doesNotContain(SECRET_KEY));
        assertThatThrownBy(() -> Credential.signature("123", " "))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("secretKey");
        assertThatThrownBy(() -> Credential.accessToken(" "))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("accessToken");
    }

    @Test
    void wrongCredentialModeThrowsSdkAuthException() {
        Credential signature = Credential.signature("123", SECRET_KEY);
        Credential token = Credential.accessToken("token-value");

        assertThatThrownBy(() -> new AccessTokenAuthProvider(signature))
                .isInstanceOf(StationOpenApiAuthException.class)
                .hasMessageContaining("access token")
                .satisfies(thrown -> assertThat(thrown.getMessage()).doesNotContain(SECRET_KEY));
        assertThatThrownBy(() -> new SignatureAuthProvider(token))
                .isInstanceOf(StationOpenApiAuthException.class)
                .hasMessageContaining("signature")
                .satisfies(thrown -> assertThat(thrown.getMessage()).doesNotContain("token-value"));
        SignatureAuthProvider providerWithBlankNonce = new SignatureAuthProvider(
                signature,
                fixedClock("1770283714727"),
                fixedNonce(" "));
        assertThatThrownBy(() -> providerWithBlankNonce.authenticate(new LinkedHashMap<>()))
                .isInstanceOf(StationOpenApiAuthException.class)
                .hasMessageContaining("nonce");
    }

    private static SdkClock fixedClock(String timestamp) {
        long millis = Long.parseLong(timestamp);
        return () -> millis;
    }

    private static NonceGenerator fixedNonce(String nonce) {
        return () -> nonce;
    }

    private static NonceGenerator sequenceNonce(String first, String second) {
        return new NonceGenerator() {
            private int index;

            @Override
            public String generate() {
                return index++ == 0 ? first : second;
            }
        };
    }

    private static String md5Upper(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }
}
