package com.deeprobotics.station.openapi.sample.web;

/**
 * 为下载响应生成安全的文件名。
 *
 * <p>只保留路径最后一段，并替换换行、引号、路径分隔符和控制字符，避免响应头注入或目录信息泄露。</p>
 */
public final class SampleDownloadNames {

    private static final String FALLBACK_NAME = "station-openapi-resource.bin";

    private SampleDownloadNames() {
    }

    /**
     * 从平台资源路径提取并清理文件名；无法得到有效名称时返回固定兜底值。
     */
    public static String fromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return FALLBACK_NAME;
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String segment = filePath.substring(lastSeparator + 1);
        if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
            return FALLBACK_NAME;
        }

        StringBuilder safeName = new StringBuilder(segment.length());
        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (character == '\r' || character == '\n' || character == '/'
                    || character == '\\' || character == '"' || Character.isISOControl(character)) {
                safeName.append('_');
            } else {
                safeName.append(character);
            }
        }
        return safeName.isEmpty() ? FALLBACK_NAME : safeName.toString();
    }
}
