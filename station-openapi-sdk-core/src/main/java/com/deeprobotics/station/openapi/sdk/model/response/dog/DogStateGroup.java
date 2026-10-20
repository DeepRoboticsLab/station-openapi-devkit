package com.deeprobotics.station.openapi.sdk.model.response.dog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 四足狗状态分组。
 *
 * <p>{@code groupName} 保留平台返回的状态分组 key，
 * {@code list} 是该分组下的状态项。</p>
 *
 * <p>SDK 不把分组 key 转换成中文显示名。接入项目如果需要本地化或自定义排序，应在业务层基于
 * {@code groupName} 和 {@code status} 建立自己的映射。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogStateGroup(
        /**
         * 状态分组 key。
         *
         * <p>常见值包括 {@code batteryData.title}、{@code locationData.title}、
         * {@code basicData.title}。该字段是平台状态分组 key，
         * 不是最终展示文案；调用方如需中文名称，应在业务层根据 key 做映射。</p>
         */
        String groupName,
        /**
         * 当前分组下的状态项列表。
         *
         * <p>列表元素顺序与平台响应一致，
         * SDK 不重新排序，避免调用方排查时和平台接口原始响应顺序不一致。</p>
         */
        List<DogStateProperty> list) {

    /**
     * 创建状态分组，并把缺失状态项归一为空的不可变列表。
     *
     * @param groupName 状态分组 key
     * @param list 状态项列表，允许为 {@code null}
     */
    public DogStateGroup {
        // 平台未返回状态项时可能为 null；公共模型统一归一为空列表，调用方遍历时无需额外判空。
        list = list == null ? List.of() : List.copyOf(list);
    }
}
