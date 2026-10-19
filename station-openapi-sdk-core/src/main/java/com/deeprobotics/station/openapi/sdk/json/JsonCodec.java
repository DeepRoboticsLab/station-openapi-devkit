package com.deeprobotics.station.openapi.sdk.json;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * SDK 内部统一 JSON 编解码器。
 *
 * <p>所有请求序列化和响应反序列化都应通过这里完成，避免不同模块各自 new ObjectMapper
 * 导致时间格式、未知字段策略或 null 字段策略不一致。签名源来自请求 {@code toMap()}，
 * JSON 序列化必须保持同一份 body，不允许在 transport 层再次加工字段。</p>
 */
public final class JsonCodec {

    /**
     * 平台接口约定的日期时间字符串格式。不要改成 ISO-8601，否则签名源和 JSON body 都会变化。
     */
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 平台接口中的本地时间字段使用 HH:mm:ss，例如周期任务开始/结束时间。
     */
    private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final JsonCodec DEFAULT = new JsonCodec(createMapper());

    /**
     * SDK 专用 ObjectMapper。
     *
     * <p>该实例只在 JsonCodec 内部使用；对外返回副本，避免客户修改全局序列化规则。</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 使用指定 mapper 创建 codec。
     *
     * <p>传入 mapper 的生命周期由调用方管理；codec 不修改其配置，但后续读写直接使用该实例。
     * 如需 SDK 默认的时间和兼容配置，应优先使用 {@link #defaultCodec()}。</p>
     *
     * @param objectMapper JSON mapper，不能为 {@code null}
     */
    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
    }

    /**
     * 返回线程安全、可复用的 SDK 默认 codec。
     *
     * @return SDK 全局默认 codec
     */
    public static JsonCodec defaultCodec() {
        return DEFAULT;
    }

    /**
     * 序列化为 JSON 字符串。异常会转成 SDK 自己的序列化异常，便于上层统一补充 path/requestId。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // Jackson 的消息及原因链可能包含属性值，不能只靠字段名脱敏；保留失败类型即可。
            throw new StationOpenApiSerializationException("Failed to serialize JSON: " + ex.getClass().getSimpleName());
        }
    }

    /**
     * 序列化为 UTF-8 字节。签名设计固定 UTF-8，HTTP body 也固定 UTF-8，避免客户机器默认编码差异。
     *
     * @param value 待序列化对象
     * @return UTF-8 JSON 字节
     */
    public byte[] writeBytes(Object value) {
        return write(value).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从 UTF-8 JSON 字符串读取普通类型。
     *
     * @param value JSON 字符串，不能为 {@code null}
     * @param type 目标类型
     * @param <T> 目标 Java 类型
     * @return 反序列化结果
     */
    public <T> T read(String value, Class<T> type) {
        // 便捷读取普通类，主要用于简单响应、测试和内部二次反序列化。
        Objects.requireNonNull(type, "type is required");
        return read(value == null ? null : value.getBytes(StandardCharsets.UTF_8), objectMapper.constructType(type));
    }

    /**
     * 从 UTF-8 JSON 字符串读取带泛型信息的类型。
     *
     * @param <T> 目标 Java 类型
     * @param value JSON 字符串，不能为 {@code null}
     * @param typeRef 保留完整泛型信息的目标类型
     * @return 反序列化结果
     */
    public <T> T read(String value, TypeRef<T> typeRef) {
        // 字符串入口统一转换为 UTF-8 字节，避免不同平台默认编码影响解析。
        return read(value == null ? null : value.getBytes(StandardCharsets.UTF_8), typeRef);
    }

    /**
     * 从 UTF-8 JSON 字节读取带泛型信息的类型。
     *
     * @param <T> 目标 Java 类型
     * @param value UTF-8 JSON 字节，不能为 {@code null}
     * @param typeRef 保留完整泛型信息的目标类型
     * @return 反序列化结果
     */
    public <T> T read(byte[] value, TypeRef<T> typeRef) {
        // 泛型响应解包必须走 TypeRef，否则 PageResult<T> 等嵌套类型会丢失元素类型。
        Objects.requireNonNull(typeRef, "typeRef is required");
        return read(value, objectMapper.constructType(typeRef.type()));
    }

    /**
     * 返回默认配置的 mapper 副本。修改副本不会影响 codec 后续读写。
     *
     * @return 当前 mapper 的独立副本
     */
    public ObjectMapper objectMapper() {
        // 返回副本，防止外部修改全局 mapper 后影响 SDK 所有请求。
        return objectMapper.copy();
    }

    private <T> T read(byte[] value, JavaType javaType) {
        if (value == null) {
            throw new StationOpenApiSerializationException("JSON body is required");
        }
        try {
            return objectMapper.readValue(value, javaType);
        } catch (IOException ex) {
            // 类型/格式错误会在异常消息中回显原值，故不保留原始消息或原因链。
            throw new StationOpenApiSerializationException("Failed to parse JSON: " + ex.getClass().getSimpleName());
        }
    }

    private static ObjectMapper createMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LOCAL_DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LOCAL_DATE_TIME_FORMATTER));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(LOCAL_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(LOCAL_TIME_FORMATTER));

        return new ObjectMapper()
                .registerModule(javaTimeModule)
                // null 不进入 JSON body，避免平台验签源和 SDK 本地签名源不一致。
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 平台新增响应字段时，已有 SDK 仍应能解析已知字段。
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
