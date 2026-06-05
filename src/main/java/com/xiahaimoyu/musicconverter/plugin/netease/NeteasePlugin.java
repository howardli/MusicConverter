package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.plugin.AbstractPlugin;
import com.xiahaimoyu.musicconverter.plugin.AudioDecoder;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;

import java.util.Collections;
import java.util.Set;

/**
 * 网易云音乐插件
 * <p>
 * 支持 NCM 加密格式的解码和元数据处理
 */
public final class NeteasePlugin extends AbstractPlugin {

    private static final Set<String> EXTENSIONS = Collections.singleton("ncm");

    @Override
    public String pluginName() {
        return "Netease Cloud Music";
    }

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected AudioDecoder createDecoderInstance() {
        return new NcmDecoder();
    }

    @Override
    protected MetadataHandler createMetadataHandlerInstance() {
        return new NcmMetadataHandler();
    }
}