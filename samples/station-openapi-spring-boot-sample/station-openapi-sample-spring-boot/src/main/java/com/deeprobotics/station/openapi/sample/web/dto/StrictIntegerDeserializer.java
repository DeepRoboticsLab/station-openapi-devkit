package com.deeprobotics.station.openapi.sample.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * 只接受 JSON 整数的 Web 入参转换器。
 *
 * <p>用于模板启停状态，禁止 Jackson 把 0.5 截断成 0，或把字符串隐式转换为控制状态。
 * 缺失值和 null 仍交由 Bean Validation 检查；不改变其他接口的 JSON 配置。</p>
 */
public final class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            // 不把原始请求值拼入异常；Web 异常处理器统一返回本地化的校验错误。
            return context.reportInputMismatch(Integer.class, "Expected an integer JSON value");
        }
        return parser.getIntValue();
    }
}
