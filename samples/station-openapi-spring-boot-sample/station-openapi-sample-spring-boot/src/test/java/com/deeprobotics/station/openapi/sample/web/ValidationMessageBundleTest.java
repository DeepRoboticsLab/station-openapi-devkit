package com.deeprobotics.station.openapi.sample.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sample.web.dto.CameraInputs;
import com.deeprobotics.station.openapi.sample.web.dto.DogInputs;
import com.deeprobotics.station.openapi.sample.web.dto.InventoryInputs;
import com.deeprobotics.station.openapi.sample.web.dto.ResultInputs;
import com.deeprobotics.station.openapi.sample.web.dto.TaskInputs;
import com.deeprobotics.station.openapi.sample.web.dto.TaskTemplateInputs;
import jakarta.validation.Constraint;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidationMessageBundleTest {

    private static final List<Class<?>> INPUT_GROUPS = List.of(
            InventoryInputs.class,
            DogInputs.class,
            CameraInputs.class,
            TaskTemplateInputs.class,
            TaskInputs.class,
            ResultInputs.class);

    @Test
    void bundlesHaveIdenticalKeysAndCoverEveryConstraintMessage()
            throws IOException, ReflectiveOperationException {
        Properties chinese = load("i18n/messages_zh_CN.properties");
        Properties english = load("i18n/messages_en_US.properties");
        Set<String> constraintKeys = constraintMessageKeys();

        assertThat(chinese.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(english.stringPropertyNames());
        assertThat(constraintKeys)
                .allMatch(key -> !key.startsWith("{jakarta.validation."))
                .allSatisfy(key -> {
                    assertThat(chinese).containsKey(key);
                    assertThat(english).containsKey(key);
                });
    }

    private static Set<String> constraintMessageKeys()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Set<String> keys = new LinkedHashSet<>();
        for (Class<?> inputGroup : INPUT_GROUPS) {
            for (Class<?> nestedType : inputGroup.getDeclaredClasses()) {
                if (!nestedType.isRecord()) {
                    continue;
                }
                Arrays.stream(nestedType.getRecordComponents())
                        .flatMap(component -> Arrays.stream(component.getAnnotations()))
                        .filter(ValidationMessageBundleTest::isConstraint)
                        .map(ValidationMessageBundleTest::messageKey)
                        .forEach(keys::add);
            }
        }
        return keys;
    }

    private static boolean isConstraint(Annotation annotation) {
        return annotation.annotationType().isAnnotationPresent(Constraint.class);
    }

    private static String messageKey(Annotation annotation) {
        try {
            // Sample 约定直接使用稳定消息键，因此这里反射读取每个约束注解的 message 属性。
            return (String) annotation.annotationType().getMethod("message").invoke(annotation);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read validation message key", exception);
        }
    }

    private static Properties load(String path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Objects.requireNonNull(
                ValidationMessageBundleTest.class.getClassLoader().getResourceAsStream(path), path);
                InputStreamReader reader = new InputStreamReader(input, UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
