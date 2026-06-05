package com.xiahaimoyu.musicconverter.plugin;

import java.util.Optional;

/**
 * 插件抽象基类
 * <p>
 * 提供模板方法模式的默认实现，简化具体插件开发
 */
public abstract class AbstractPlugin implements FormatPlugin {

    @Override
    public AudioDecoder createDecoder() {
        return createDecoderInstance();
    }

    @Override
    public Optional<MetadataHandler> createMetadataHandler() {
        MetadataHandler handler = createMetadataHandlerInstance();
        return Optional.ofNullable(handler);
    }

    /**
     * 创建解码器实例（子类必须实现）
     *
     * @return 音频解码器
     */
    protected abstract AudioDecoder createDecoderInstance();

    /**
     * 创建元数据处理器实例（子类可选实现）
     * <p>
     * 默认返回 null，表示不需要处理元数据
     *
     * @return 元数据处理器，或 null
     */
    protected MetadataHandler createMetadataHandlerInstance() {
        return null;
    }
}