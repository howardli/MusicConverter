package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.ConversionResult;

import java.nio.file.Path;

/**
 * 解码器接口
 * <p>
 * 负责将加密音频文件解码为标准格式
 */
public interface Decoder {

    /**
     * 解码音频文件
     *
     * @param source 源文件路径
     * @param output 输出文件路径基础名（不含扩展名，解码器需根据格式添加）
     * @return 解码结果，包含输出文件和元数据；失败返回 ConversionResult.failure()
     * @throws ConverterException 解码过程中发生错误
     */
    ConversionResult decode(Path source, Path output) throws ConverterException;
}