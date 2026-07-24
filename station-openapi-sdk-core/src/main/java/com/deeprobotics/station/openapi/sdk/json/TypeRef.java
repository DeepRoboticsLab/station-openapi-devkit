package com.deeprobotics.station.openapi.sdk.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 捕获泛型响应类型的轻量工具，类似 Jackson 的 {@code TypeReference}。
 *
 * <p>SDK 解包 {@code ApiResponse<PageResult<DogInfo>>} 这类嵌套泛型时，运行期必须保留泛型信息。
 * 使用方式是创建匿名子类：{@code new TypeRef<ApiResponse<String>>() {}}。</p>
 */
public abstract class TypeRef<T> {

    /**
     * 匿名子类捕获到的真实泛型类型。
     */
    private final Type type;

    /**
     * 从匿名子类的泛型父类声明中捕获 {@code T} 的运行期类型。
     *
     * @throws IllegalArgumentException 直接使用原始 {@code TypeRef}、导致泛型信息缺失时抛出
     */
    protected TypeRef() {
        Type superType = getClass().getGenericSuperclass();
        if (!(superType instanceof ParameterizedType parameterizedType)) {
            // 直接 new TypeRef() 会丢失 T，反序列化结果不可预测，所以这里尽早失败。
            throw new IllegalArgumentException("TypeRef must be created with generic type information");
        }
        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    Type type() {
        return type;
    }
}
