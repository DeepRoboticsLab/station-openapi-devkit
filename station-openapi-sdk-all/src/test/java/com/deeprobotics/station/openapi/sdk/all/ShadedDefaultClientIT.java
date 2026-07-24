package com.deeprobotics.station.openapi.sdk.all;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 仅从最终 shaded JAR 加载 SDK，验证 SPI 元数据和 relocated 运行时依赖都可独立工作。
 */
class ShadedDefaultClientIT {

    @Test
    void shadedJarDiscoversDefaultTransportAndCallsTime() throws Exception {
        Path shadedJar = Path.of(System.getProperty("station.openapi.shaded.jar"));
        assertTrue(Files.isRegularFile(shadedJar), "shaded JAR must exist before integration-test");

        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> accessToken = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/remoteApi/time", exchange -> handleTimeRequest(
                exchange, requestPath, accessToken, requestBody));
        server.start();

        Thread thread = Thread.currentThread();
        ClassLoader originalContextLoader = thread.getContextClassLoader();
        URL[] urls = {shadedJar.toUri().toURL()};
        try (URLClassLoader isolated = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            thread.setContextClassLoader(isolated);
            Class<?> clientsClass = Class.forName(
                    "com.deeprobotics.station.openapi.sdk.StationOpenApiClients", true, isolated);
            assertSame(isolated, clientsClass.getClassLoader());

            Object builder = invoke(null, clientsClass.getMethod("builder"));
            invoke(builder, builder.getClass().getMethod("endpoint", String.class),
                    "http://127.0.0.1:" + server.getAddress().getPort());
            invoke(builder, builder.getClass().getMethod("accessToken", String.class), "shaded-token");
            Object client = invoke(builder, builder.getClass().getMethod("build"));
            try {
                Object systemApi = invoke(client, client.getClass().getMethod("system"));
                String timeText = (String) invoke(systemApi, systemApi.getClass().getMethod("getTimeText"));
                assertEquals("2026-07-10T21:00:00", timeText);
            } finally {
                invoke(client, client.getClass().getMethod("close"));
            }
        } finally {
            thread.setContextClassLoader(originalContextLoader);
            server.stop(0);
        }

        assertEquals("/remoteApi/time", requestPath.get());
        assertEquals("shaded-token", accessToken.get());
        assertEquals("{}", requestBody.get());
    }

    private static void handleTimeRequest(HttpExchange exchange, AtomicReference<String> requestPath,
            AtomicReference<String> accessToken, AtomicReference<String> requestBody) throws IOException {
        requestPath.set(exchange.getRequestURI().getPath());
        accessToken.set(exchange.getRequestHeaders().getFirst("X-Access-Token"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] response = ("{\"success\":true,\"code\":200,\"msg\":\"OK\","
                + "\"data\":\"2026-07-10T21:00:00\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static Object invoke(Object target, Method method, Object... arguments) throws Exception {
        return method.invoke(target, arguments);
    }
}
