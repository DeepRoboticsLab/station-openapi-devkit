package com.deeprobotics.station.openapi.sdk.rocketmq;

import java.net.URL;
import java.nio.file.Path;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RuntimeDependencyVersionTest {

    @Test
    void runtimeDependenciesUseFrozenSecurityVersions() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(codeSourceJar("com.fasterxml.jackson.databind.ObjectMapper"))
                .as("Jackson Databind runtime artifact")
                .isEqualTo("jackson-databind-2.18.9.jar");
        softly.assertThat(codeSourceJar("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer"))
                .as("RocketMQ client runtime artifact")
                .isEqualTo("rocketmq-client-5.5.0.jar");
        softly.assertThat(codeSourceJar("io.netty.buffer.ByteBuf"))
                .as("Netty runtime artifact")
                .isEqualTo("netty-buffer-4.1.136.Final.jar");
        softly.assertThat(codeSourceJar("io.grpc.internal.GrpcUtil"))
                .as("gRPC core runtime artifact")
                .isEqualTo("grpc-core-1.82.2.jar");
        softly.assertThat(codeSourceJar("io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder"))
                .as("gRPC shaded Netty runtime artifact")
                .isEqualTo("grpc-netty-shaded-1.82.2.jar");
        softly.assertThat(codeSourceJar("org.apache.commons.beanutils.BeanUtilsBean"))
                .as("Commons BeanUtils runtime artifact")
                .isEqualTo("commons-beanutils-1.11.0.jar");
        softly.assertThat(codeSourceJar("com.google.common.collect.ImmutableList"))
                .as("Guava runtime artifact")
                .isEqualTo("guava-33.5.0-jre.jar");
        softly.assertThat(codeSourceJar("okio.Buffer"))
                .as("Okio runtime artifact")
                .isEqualTo("okio-jvm-3.6.0.jar");

        softly.assertAll();
    }

    private static String codeSourceJar(String className) throws Exception {
        Class<?> type = Class.forName(className);
        URL location = type.getProtectionDomain().getCodeSource().getLocation();
        return Path.of(location.toURI()).getFileName().toString();
    }
}
