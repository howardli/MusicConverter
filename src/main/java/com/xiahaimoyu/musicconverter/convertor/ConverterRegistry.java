package com.xiahaimoyu.musicconverter.convertor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 转换器注册表
 * <p>
 * 线程安全，支持运行时动态注册
 */
public final class ConverterRegistry {

    private final Map<String, MusicConverter> registry = new ConcurrentHashMap<>();

    public ConverterRegistry register(MusicConverter converter) {
        converter.supportedExtensions().forEach(ext ->
                registry.put(ext.toLowerCase(), converter));
        return this;
    }

    public Optional<MusicConverter> find(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(extension.toLowerCase()));
    }

    public boolean supports(String extension) {
        if (extension == null) {
            return false;
        }
        return registry.containsKey(extension.toLowerCase());
    }

    public Set<String> allExtensions() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public static ConverterRegistry of(MusicConverter... converters) {
        ConverterRegistry instance = new ConverterRegistry();
        for (MusicConverter converter : converters) {
            instance.register(converter);
        }
        return instance;
    }
}