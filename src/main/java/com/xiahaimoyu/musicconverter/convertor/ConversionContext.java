package com.xiahaimoyu.musicconverter.convertor;

import java.nio.file.Path;

/**
 * 转换上下文，用于在转换流程中传递数据
 */
public final class ConversionContext {

    private final Path tempFile;
    private final Object metadata;

    private ConversionContext(Path tempFile, Object metadata) {
        this.tempFile = tempFile;
        this.metadata = metadata;
    }

    public static ConversionContext of(Path tempFile) {
        return new ConversionContext(tempFile, null);
    }

    public static ConversionContext of(Path tempFile, Object metadata) {
        return new ConversionContext(tempFile, metadata);
    }

    public Path tempFile() {
        return tempFile;
    }

    @SuppressWarnings("unchecked")
    public <T> T metadata() {
        return (T) metadata;
    }

    public boolean isValid() {
        return tempFile != null;
    }
}