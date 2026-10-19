package com.deeprobotics.station.openapi.sample.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.deeprobotics.station.openapi.sample.service.ResultSampleService;
import com.deeprobotics.station.openapi.sample.web.dto.ResultInputs;
import com.deeprobotics.station.openapi.sample.web.view.PageView;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 巡检结果查询与资源下载的薄 Web 示例，对应 SDK 的 {@code client.result()} 分组。
 *
 * <p>通过 {@link StreamingResponseBody} 将平台数据直接转发到 Servlet 输出流，不暂存、不重试。
 * 响应提交后若下载中断，客户端可能收到部分文件，无法再转换为完整 ProblemDetail 响应。</p>
 */
@RestController
@RequestMapping("/sample/api/result")
@Tag(name = "Result", description = "巡检结果与资源下载 / Inspection results and resources")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class ResultSampleController {

    private final ResultSampleService service;

    public ResultSampleController(ResultSampleService service) {
        this.service = service;
    }

    @PostMapping("/page-task-point-results")
    @Operation(operationId = "result.pageTaskPointResults",
            summary = "分页查询巡检点结果 / Page inspection point results")
    public PageView<PatrolResultInfo> pageTaskPointResults(
            @Valid @RequestBody ResultInputs.PageTaskPointResults input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pageTaskPointResults(
                input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/download-resource")
    @Operation(operationId = "result.downloadResource", summary = "流式下载结果资源 / Stream result resource")
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @Valid @RequestBody ResultInputs.DownloadResource input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        RequestOptions options = SampleRequestOptions.from(requestId);
        // Servlet 输出流直接交给 SDK，避免 Sample 为大文件建立额外内存缓冲。
        StreamingResponseBody body = outputStream ->
                service.download(input.toSdkRequest(), outputStream, options);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(SampleDownloadNames.fromPath(input.filePath()), UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
