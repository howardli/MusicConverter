package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.plugin.AbstractPlugin;
import com.xiahaimoyu.musicconverter.plugin.Decoder;

import java.util.Set;

/**
 * 直接复制插件
 * <p>
 * 处理已解密的普通音频格式（MP3、FLAC 等），直接复制文件
 */
public final class CopyPlugin extends AbstractPlugin {

    private static final Set<String> EXTENSIONS = Set.of(
            "mp3", "flac", "wav", "aac", "ogg", "m4a");

    @Override
    public String pluginName() {
        return "Direct Copy";
    }

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected Decoder createDecoderInstance() {
        return new CopyDecoder();
    }
}