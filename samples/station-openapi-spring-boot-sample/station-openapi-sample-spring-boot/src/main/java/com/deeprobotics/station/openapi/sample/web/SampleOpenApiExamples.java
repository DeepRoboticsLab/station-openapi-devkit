package com.deeprobotics.station.openapi.sample.web;

/**
 * Swagger 中复用的双语 ProblemDetail 示例。
 *
 * <p>示例只展示稳定错误结构，不得放入鉴权凭证、平台原始响应体或现场设备敏感信息。</p>
 */
final class SampleOpenApiExamples {

    static final String VALIDATION_ZH = """
            {"title":"参数校验失败","status":400,"detail":"查询四足狗状态时 dogCode 不能为空","code":"SAMPLE_VALIDATION_FAILED","field":"dogCode"}
            """;

    static final String VALIDATION_EN = """
            {"title":"Validation failed","status":400,"detail":"dogCode is required when querying robot dog status.","code":"SAMPLE_VALIDATION_FAILED","field":"dogCode"}
            """;

    static final String DANGEROUS_ZH = """
            {"title":"危险操作已禁用","status":403,"detail":"危险操作默认关闭，请显式启用后再调用","code":"SAMPLE_DANGEROUS_OPERATION_DISABLED"}
            """;

    static final String DANGEROUS_EN = """
            {"title":"Dangerous operation disabled","status":403,"detail":"Dangerous operations are disabled by default; enable them explicitly before calling","code":"SAMPLE_DANGEROUS_OPERATION_DISABLED"}
            """;

    private SampleOpenApiExamples() {
    }
}
