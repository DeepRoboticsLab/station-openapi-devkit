package com.deeprobotics.station.openapi.sample.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sample.web.view.CameraStreamView;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InventoryInputsTest {

    @Test
    void pageDogsMapsEveryFieldWithoutLegacyPageNumber() {
        assertThat(new InventoryInputs.PageDogs(1, 20, "X30", "X30").toSdkRequest().toMap())
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 20)
                .containsEntry("dogName", "X30")
                .containsEntry("deviceMode", "X30")
                .doesNotContainKey("pageNum");
    }

    @Test
    void pageDogsLeavesPageSizeNormalizationToSdk() {
        assertThat(new InventoryInputs.PageDogs(null, 0, null, null).toSdkRequest().toMap())
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 10);
    }

    @Test
    void mapsMapsBothOptionalFilters() {
        assertThat(new InventoryInputs.Maps("F1", "dog-001").toSdkRequest().toMap())
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("floor", "F1"),
                        org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void pageStreamsMapsFiltersAndPagination() {
        assertThat(new InventoryInputs.PageStreams("front", "dog-001", "visible", 2, 30)
                .toSdkRequest().toMap())
                .containsEntry("name", "front")
                .containsEntry("bindDeviceCode", "dog-001")
                .containsEntry("videoMode", "visible")
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 30);
    }

    @Test
    void pagePointsMapsFiltersAndPagination() {
        assertThat(new InventoryInputs.PagePoints("point-001", "dog-001", 3, 40)
                .toSdkRequest().toMap())
                .containsEntry("pointCode", "point-001")
                .containsEntry("dogCode", "dog-001")
                .containsEntry("pageNo", 3)
                .containsEntry("pageSize", 40);
    }

    @Test
    void maintainAreasDelegatesTimeFormattingToSdkRequest() {
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 13, 8, 9, 10);
        LocalDateTime endTime = LocalDateTime.of(2026, 7, 14, 18, 19, 20);

        assertThat(new InventoryInputs.PageMaintainAreas(
                "dog-001", "area-001", "repair", startTime, endTime, 1, "F1", 4, 50)
                .toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("maintainAreaCode", "area-001")
                .containsEntry("repairDesc", "repair")
                .containsEntry("startTime", "2026-07-13 08:09:10")
                .containsEntry("endTime", "2026-07-14 18:19:20")
                .containsEntry("issued", 1)
                .containsEntry("floor", "F1")
                .containsEntry("pageNo", 4)
                .containsEntry("pageSize", 50);
    }

    @Test
    void roadMapsMapsRequiredDogCode() {
        assertThat(new InventoryInputs.RoadMaps("dog-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void docksMapsRequiredDogCode() {
        assertThat(new InventoryInputs.Docks("dog-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void nodesMapsRequiredDogCode() {
        assertThat(new InventoryInputs.Nodes("dog-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void cameraStreamViewNeverDeclaresPassword() {
        CameraStreamInfo source = new CameraStreamInfo(
                "front", "camera-001", "dog-001", "vendor", "model", "patrol", "visible",
                "127.0.0.1", "554", "viewer", "secret-password", "1", "main", "rtsp://camera");

        CameraStreamView view = CameraStreamView.from(source);

        assertThat(view.name()).isEqualTo("front");
        assertThat(view.streamUrl()).isEqualTo("rtsp://camera");
        assertThat(Arrays.stream(CameraStreamView.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password");
        assertThat(view.toString()).doesNotContain("secret-password");
    }
}
