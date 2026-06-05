package com.xiahaimoyu.musicconverter.plugin;

import java.util.Optional;
import java.util.Set;

/**
 * 格式插件主接口
 * <p>
 * 实现此接口以支持新的加密音乐格式，每个插件负责一种或多种格式
 */
public interface FormatPlugin {

    /**
     * 返回支持的文件扩展名（小写，无点号）
     *
     * @return 扩展名集合
     */
    Set<String> supportedExtensions();

    /**
     * 创建解码器实例
     *
     * @return 音频解码器
     */
    AudioDecoder createDecoder();

    /**
     * 创建元数据处理实例（可选）
     * <p>
     * 某些格式不需要写入元数据（如直接复制），可返回空
     *
     * @return 元数据处理器，或 Optional.empty()
     */
    Optional<MetadataHandler> createMetadataHandler();

    /**
     * 插件名称，用于日志和诊断
     *
     * @return 插件名称
     */
    String pluginName();
}