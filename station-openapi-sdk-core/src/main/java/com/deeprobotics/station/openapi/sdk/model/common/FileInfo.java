package com.deeprobotics.station.openapi.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * 巡检结果中的文件描述。
 *
 * <p>字段形态与平台巡检结果中的文件对象一致：{@code filePath} 可作为
 * {@code /remoteApi/result/resource} 的下载入参，{@code title/fileType} 用于客户展示和分类。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FileInfo {

    /**
     * 文件路径或文件名，可作为资源下载接口的 {@code filePath} 入参。
     */
    private final String filePath;
    /**
     * 文件展示名称，通常用于客户页面或报表展示。
     */
    private final String title;
    /**
     * 文件类型编码或后缀描述，按平台原值透传。
     */
    private final String fileType;

    /**
     * 创建文件描述，主要由 Jackson 反序列化调用。
     *
     * @param filePath 平台侧文件路径或文件名
     * @param title 文件展示名称
     * @param fileType 平台文件类型原值
     */
    @JsonCreator
    public FileInfo(@JsonProperty("filePath") String filePath, @JsonProperty("title") String title,
            @JsonProperty("fileType") String fileType) {
        this.filePath = filePath;
        this.title = title;
        this.fileType = fileType;
    }

    /**
     * 返回平台侧文件路径。
     *
     * @return 可传给资源下载接口的路径
     */
    public String filePath() {
        return filePath;
    }

    /**
     * 返回文件展示名称。
     *
     * @return 平台 title 原值
     */
    public String title() {
        return title;
    }

    /**
     * 返回平台文件类型原值。
     *
     * @return 平台 fileType 原值
     */
    public String fileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return "FileInfo{"
                + "filePath='" + filePath + '\''
                + ", title='" + title + '\''
                + ", fileType='" + fileType + '\''
                + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FileInfo fileInfo)) {
            return false;
        }
        return Objects.equals(filePath, fileInfo.filePath)
                && Objects.equals(title, fileInfo.title)
                && Objects.equals(fileType, fileInfo.fileType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, title, fileType);
    }
}
