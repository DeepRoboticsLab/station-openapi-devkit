package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.internal.DefaultCameraApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultDogApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultInventoryApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultResultApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultSystemApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskTemplateApi;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * RemoteApi HTTP 契约覆盖闸门。
 *
 * <p>该测试直接读取各业务 API 默认实现中的 {@link EndpointDefinition} 元数据，避免只通过
 * 文档或字符串搜索判断覆盖情况。新增、删除、改名任意一个生产 path，都必须同步调整本测试并人工确认。</p>
 */
class OpenApiCoverageTest {

    private static final String INTERNAL_PACKAGE = "com.deeprobotics.station.openapi.sdk.internal";
    private static final String INTERNAL_PACKAGE_PATH = INTERNAL_PACKAGE.replace('.', '/');

    private static final List<Class<?>> API_IMPLEMENTATIONS = List.of(
            DefaultInventoryApi.class,
            DefaultResultApi.class,
            DefaultDogApi.class,
            DefaultCameraApi.class,
            DefaultTaskTemplateApi.class,
            DefaultTaskApi.class,
            DefaultSystemApi.class);

    private static final List<String> PRODUCTION_PATHS = List.of(
            "/remoteApi/dogPage",
            "/remoteApi/mapList",
            "/remoteApi/streamPage",
            "/remoteApi/pointPage",
            "/remoteApi/maintainAreaPage",
            "/remoteApi/roadMapList",
            "/remoteApi/dockList",
            "/remoteApi/nodeList",
            "/remoteApi/result/resource",
            "/remoteApi/getDogStateData",
            "/remoteApi/chargeCtrl",
            "/remoteApi/controlDog",
            "/remoteApi/setPosToDog",
            "/remoteApi/alarmPage",
            "/remoteApi/cameraCtrl",
            "/remoteApi/ptzReset",
            "/remoteApi/onlySaveTaskModel",
            "/remoteApi/switchTaskModelStatus",
            "/remoteApi/removeTaskModelByCodes",
            "/remoteApi/taskModelByCode",
            "/remoteApi/modelPage",
            "/remoteApi/issue",
            "/remoteApi/taskCtrl",
            "/remoteApi/batchCancelTask",
            "/remoteApi/taskPage",
            "/remoteApi/taskPointResultPage",
            "/remoteApi/dogPos",
            "/remoteApi/time");

    private static final List<String> FORBIDDEN_PATHS = List.of(
            "/remoteApi/sign",
            "/remoteApi/app/create",
            "/remoteApi/app/resetSecret");

    @Test
    void allDefaultApiImplementationsAreCoveredByThisGate() {
        List<String> discoveredClassNames = classNames(discoverDefaultApiImplementations());
        List<String> coveredClassNames = classNames(API_IMPLEMENTATIONS);

        assertThat(coveredClassNames)
                .as("Every Default*Api implementation must be included in the endpoint coverage gate")
                .containsExactlyElementsOf(discoveredClassNames)
                .doesNotHaveDuplicates();
    }

    @Test
    void endpointMetadataContainsExactlyTwentyEightProductionPaths() {
        List<String> actualPaths = endpointDefinitions()
                .map(EndpointDefinition::path)
                .toList();

        assertThat(actualPaths)
                .as("SDK must expose exactly the 28 approved production RemoteApi paths")
                .containsExactlyInAnyOrderElementsOf(PRODUCTION_PATHS)
                .doesNotHaveDuplicates()
                .hasSize(28);
    }

    @Test
    void endpointMetadataOnlyContainsRemoteApiPaths() {
        List<String> actualPaths = endpointDefinitions()
                .map(EndpointDefinition::path)
                .toList();

        assertThat(actualPaths)
                .as("Endpoint metadata in business APIs must stay under /remoteApi")
                .allSatisfy(path -> assertThat(path).startsWith("/remoteApi/"));
    }

    @Test
    void endpointMetadataDoesNotExposeCredentialManagementPaths() {
        List<String> actualPaths = endpointDefinitions()
                .map(EndpointDefinition::path)
                .toList();

        assertThat(actualPaths)
                .as("SDK must not expose backend signing or credential-management APIs")
                .doesNotContain(FORBIDDEN_PATHS.toArray(String[]::new));
    }

    private static List<String> classNames(List<Class<?>> types) {
        return types.stream()
                .map(Class::getName)
                .sorted()
                .toList();
    }

    private static List<Class<?>> discoverDefaultApiImplementations() {
        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(INTERNAL_PACKAGE_PATH);
            List<Class<?>> discovered = new ArrayList<>();
            while (resources.hasMoreElements()) {
                discovered.addAll(discoverDefaultApiImplementations(resources.nextElement()));
            }
            if (discovered.isEmpty()) {
                throw new AssertionError("Cannot find compiled Default*Api classes under " + INTERNAL_PACKAGE);
            }
            return discovered.stream().distinct().toList();
        } catch (IOException ex) {
            throw new AssertionError("Failed to discover Default*Api implementations under " + INTERNAL_PACKAGE, ex);
        }
    }

    private static List<Class<?>> discoverDefaultApiImplementations(URL packageUrl) {
        if (!"file".equals(packageUrl.getProtocol())) {
            throw new AssertionError("Unsupported classpath protocol for " + INTERNAL_PACKAGE + ": "
                    + packageUrl.getProtocol());
        }
        try (Stream<Path> files = Files.list(Path.of(packageUrl.toURI()))) {
            return files
                    .filter(path -> path.getFileName().toString().matches("Default.+Api\\.class"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.class$", ""))
                    .map(OpenApiCoverageTest::loadInternalClass)
                    .toList();
        } catch (IOException | URISyntaxException ex) {
            throw new AssertionError("Failed to inspect compiled package directory " + packageUrl, ex);
        }
    }

    private static Class<?> loadInternalClass(String simpleName) {
        try {
            return Class.forName(INTERNAL_PACKAGE + "." + simpleName);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Failed to load " + INTERNAL_PACKAGE + "." + simpleName, ex);
        }
    }

    private static Stream<EndpointDefinition> endpointDefinitions() {
        return API_IMPLEMENTATIONS.stream()
                .flatMap(OpenApiCoverageTest::endpointDefinitions);
    }

    private static Stream<EndpointDefinition> endpointDefinitions(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
                .filter(field -> field.getType() == EndpointDefinition.class)
                .map(OpenApiCoverageTest::readEndpointDefinition);
    }

    private static EndpointDefinition readEndpointDefinition(Field field) {
        try {
            field.setAccessible(true);
            return (EndpointDefinition) field.get(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Failed to read endpoint metadata from " + field.getDeclaringClass().getName()
                    + "." + field.getName(), ex);
        }
    }
}
