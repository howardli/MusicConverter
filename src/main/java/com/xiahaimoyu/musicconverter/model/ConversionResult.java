package com.xiahaimoyu.musicconverter.model;

import java.nio.file.Path;

/**
 * 转换结果
 * <p>
 * 包含输出文件路径和提取的元数据
 */
public final class ConversionResult {

    private final Path outputFile;
    private final AudioMetadata metadata;
    private final boolean success;

    private ConversionResult(Path outputFile, AudioMetadata metadata, boolean success) {
        this.outputFile = outputFile;
        this.metadata = metadata;
        this.success = success;
    }

    /**
     * 创建成功结果
     */
    public static ConversionResult success(Path outputFile, AudioMetadata metadata) {
        return new ConversionResult(outputFile, metadata, true);
    }

    /**
     * 创建失败结果
     */
    public static ConversionResult failure() {
        return new ConversionResult(null, null, false);
    }

    public Path outputFile() {
        return outputFile;
    }

    public AudioMetadata metadata() {
        return metadata;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean hasMetadata() {
        return metadata != null;
    }
}