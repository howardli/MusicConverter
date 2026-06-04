package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;

import java.nio.file.Path;

/**
 * 元数据处理接口
 * <p>
 * 负责将元数据写入音频文件
 */
public interface MetadataHandler {

    /**
     * 将元数据写入音频文件
     *
     * @param file   音频文件路径
     * @param metadata 元数据
     * @throws ConverterException 写入过程中发生错误
     */
    void writeMetadata(Path file, AudioMetadata metadata) throws ConverterException;
}