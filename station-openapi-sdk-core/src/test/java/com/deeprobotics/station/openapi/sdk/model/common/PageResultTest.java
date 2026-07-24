package com.deeprobotics.station.openapi.sdk.model.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 两类平台分页响应结构的统一解析测试。
 *
 * <p>完整分页响应提供 current/size/pages；{@code streamPage} 响应可以只有 records/total。
 * 测试确保缺失页码字段和 records 都能按公共模型约定安全处理。</p>
 */
class PageResultTest {

    private final JsonCodec json = JsonCodec.defaultCodec();

    @Test
    void parsesMyBatisPlusPageJson() {
        String body = "{\"records\":[{\"code\":\"DOG-001\"}],\"total\":21,\"current\":2,\"size\":10,"
                + "\"pages\":3,\"orders\":[],\"searchCount\":true,\"optimizeCountSql\":true}";

        PageResult<DogRecord> result = json.read(body, new TypeRef<PageResult<DogRecord>>() {
        });

        assertThat(result.records()).extracting(DogRecord::code).containsExactly("DOG-001");
        assertThat(result.total()).isEqualTo(21);
        assertThat(result.pageNo()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.pages()).isEqualTo(3);
    }

    @Test
    void parsesThePageJsonWithRecordsAndTotalOnly() {
        String body = "{\"records\":[{\"code\":\"STREAM-001\"}],\"total\":1}";

        PageResult<DogRecord> result = json.read(body, new TypeRef<PageResult<DogRecord>>() {
        });

        assertThat(result.records()).extracting(DogRecord::code).containsExactly("STREAM-001");
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.pageNo()).isNull();
        assertThat(result.pageSize()).isNull();
        assertThat(result.pages()).isNull();
    }

    @Test
    void missingRecordsMapsToEmptyList() {
        String body = "{\"total\":0}";

        PageResult<DogRecord> result = json.read(body, new TypeRef<PageResult<DogRecord>>() {
        });

        assertThat(result.records()).isEmpty();
        assertThat(result.records()).isEqualTo(List.of());
    }

    static final class DogRecord {
        private String code;

        public String code() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
