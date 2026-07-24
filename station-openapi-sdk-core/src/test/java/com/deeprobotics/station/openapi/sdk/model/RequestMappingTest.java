package com.deeprobotics.station.openapi.sdk.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 请求模型公共 wire 规则测试。
 *
 * <p>验证 LinkedHashMap、null/空字符串、分页归一、防御性集合复制，以及 JSON 数组和英文逗号字符串
 * 两类集合字段不能混淆。该测试是所有具体请求模型的基础契约补充。</p>
 */
class RequestMappingTest {

    @Test
    void requestModelsReturnLinkedHashMapAndOmitNullFieldsButKeepEmptyStrings() {
        LinkedHashMap<String, Object> body = new DogPageRequest(null, 20, "", null).toMap();

        assertThat(body).isExactlyInstanceOf(LinkedHashMap.class);
        assertThat(body)
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 20)
                .containsEntry("dogName", "")
                .doesNotContainKey("deviceMode");
    }

    @Test
    void pageNoDefaultsToOneAndRejectsValuesLessThanOne() {
        assertThat(new DogPageRequest(null, 20, null, null).toMap())
                .containsEntry("pageNo", 1);

        assertThatThrownBy(() -> new DogPageRequest(0, 20, null, null).toMap())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pageNo");

        assertThatThrownBy(() -> new DogPageRequest(-1, 20, null, null).toMap())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pageNo");
    }

    @Test
    void pageSizeDefaultsToTenForNullZeroOrNegativeAndCapsAtOneHundred() {
        assertThat(new DogPageRequest(1, null, null, null).toMap())
                .containsEntry("pageSize", 10);
        assertThat(new DogPageRequest(1, 0, null, null).toMap())
                .containsEntry("pageSize", 10);
        assertThat(new DogPageRequest(1, -5, null, null).toMap())
                .containsEntry("pageSize", 10);
        assertThat(new DogPageRequest(1, 101, null, null).toMap())
                .containsEntry("pageSize", 100);
    }

    @Test
    void dogPageRequestEmitsPageNoAndPageSizeButNeverPageNum() {
        LinkedHashMap<String, Object> body = new DogPageRequest(2, 15, "X30", "M20").toMap();

        assertThat(body)
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 15)
                .doesNotContainKey("pageNum");
    }

    @Test
    void collectionFieldsKeepJsonArraysOrBecomeCommaStringsByWireContract() {
        LinkedHashMap<String, Object> body = new CollectionWireShapeRequest(
                List.of("T001", "T002"),
                List.of("E001", "E002"),
                List.of("P001", "P002"),
                List.of("1", "2"),
                List.of("MON", "WED"),
                List.of("RUNNING", "DONE"))
                .toMap();

        assertThat(body.get("taskCodes")).isEqualTo(List.of("T001", "T002"));
        assertThat(body.get("taskExecuteCodeList")).isEqualTo(List.of("E001", "E002"));
        assertThat(body)
                .containsEntry("pointCodes", "P001,P002")
                .containsEntry("cycleMonth", "1,2")
                .containsEntry("cycleWeek", "MON,WED")
                .containsEntry("statusArr", "RUNNING,DONE");
    }

    @Test
    void copyWithoutNullsDefensivelyCopiesCollectionValues() {
        List<String> taskCodes = new ArrayList<>(List.of("T001", "T002"));
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        source.put("taskCodes", taskCodes);

        LinkedHashMap<String, Object> copy = MapUtils.copyWithoutNulls(source);
        taskCodes.add("T003");

        assertThat(copy.get("taskCodes")).isEqualTo(List.of("T001", "T002"));
    }

    private static final class DogPageRequest extends PageRequestSupport {
        private final Integer pageNo;
        private final Integer pageSize;
        private final String dogName;
        private final String deviceMode;

        private DogPageRequest(Integer pageNo, Integer pageSize, String dogName, String deviceMode) {
            this.pageNo = pageNo;
            this.pageSize = pageSize;
            this.dogName = dogName;
            this.deviceMode = deviceMode;
        }

        @Override
        public LinkedHashMap<String, Object> toMap() {
            LinkedHashMap<String, Object> body = MapUtils.newBody();
            putPage(body, pageNo, pageSize);
            MapUtils.putIfNotNull(body, "dogName", dogName);
            MapUtils.putIfNotNull(body, "deviceMode", deviceMode);
            return body;
        }
    }

    private static final class CollectionWireShapeRequest implements OpenApiRequest {
        private final List<String> taskCodes;
        private final List<String> taskExecuteCodeList;
        private final List<String> pointCodes;
        private final List<String> cycleMonth;
        private final List<String> cycleWeek;
        private final List<String> statusArr;

        private CollectionWireShapeRequest(List<String> taskCodes, List<String> taskExecuteCodeList,
                List<String> pointCodes, List<String> cycleMonth, List<String> cycleWeek, List<String> statusArr) {
            this.taskCodes = taskCodes;
            this.taskExecuteCodeList = taskExecuteCodeList;
            this.pointCodes = pointCodes;
            this.cycleMonth = cycleMonth;
            this.cycleWeek = cycleWeek;
            this.statusArr = statusArr;
        }

        @Override
        public LinkedHashMap<String, Object> toMap() {
            LinkedHashMap<String, Object> body = MapUtils.newBody();
            MapUtils.putIfNotNull(body, "taskCodes", taskCodes);
            MapUtils.putIfNotNull(body, "taskExecuteCodeList", taskExecuteCodeList);
            MapUtils.putCommaJoined(body, "pointCodes", pointCodes);
            MapUtils.putCommaJoined(body, "cycleMonth", cycleMonth);
            MapUtils.putCommaJoined(body, "cycleWeek", cycleWeek);
            MapUtils.putCommaJoined(body, "statusArr", statusArr);
            return body;
        }
    }
}
