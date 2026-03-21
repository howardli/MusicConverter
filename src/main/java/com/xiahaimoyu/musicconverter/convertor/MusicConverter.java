package com.xiahaimoyu.musicconverter.convertor;

import java.nio.file.Path;
import java.util.Set;

/**
 * 音乐格式转换器接口
 * <p>
 * 实现此类以支持新的加密音乐格式
 */
public interface MusicConverter {

    /**
     * 返回支持的文件扩展名（小写，无点号）
     */
    Set<String> supportedExtensions();

    /**
     * 转换音乐文件
     *
     * @param source     源文件路径
     * @param targetDir  目标目录
     * @return 转换上下文，包含临时文件路径和元数据；失败返回 null
     */
    ConversionContext convert(Path source, Path targetDir) throws Exception;

    /**
     * 转换后处理（如写入元数据）
     */
    default void postProcess(ConversionContext context) throws Exception {
        // 默认无操作
    }

    /**
     * 处理单个音乐文件（包含完整的转换流程）
     *
     * @param source     源文件路径
     * @param sourceRoot 源目录根路径
     * @param targetRoot 目标目录根路径
     */
    void process(Path source, Path sourceRoot, Path targetRoot);
}