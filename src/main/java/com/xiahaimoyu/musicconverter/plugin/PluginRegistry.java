package com.xiahaimoyu.musicconverter.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 插件注册表
 * <p>
 * 线程安全，支持运行时动态注册，按扩展名查找插件
 */
public final class PluginRegistry {

    private final Map<String, FormatPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * 注册插件
     *
     * @param plugin 格式插件
     * @return this（链式调用）
     */
    public PluginRegistry register(FormatPlugin plugin) {
        plugin.supportedExtensions()
                .forEach(ext -> plugins.put(ext.toLowerCase(), plugin));
        return this;
    }

    /**
     * 查找支持指定扩展名的插件
     *
     * @param extension 文件扩展名（小写，无点号）
     * @return 插件，或 Optional.empty()
     */
    public Optional<FormatPlugin> find(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(plugins.get(extension.toLowerCase()));
    }

    /**
     * 判断是否支持指定扩展名
     *
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public boolean supports(String extension) {
        if (extension == null) {
            return false;
        }
        return plugins.containsKey(extension.toLowerCase());
    }

    /**
     * 获取所有支持的扩展名
     *
     * @return 扩展名集合
     */
    public Set<String> allExtensions() {
        return Collections.unmodifiableSet(plugins.keySet());
    }

    /**
     * 创建注册表并批量注册插件
     *
     * @param plugins 插件数组
     * @return 注册表实例
     */
    public static PluginRegistry of(FormatPlugin... plugins) {
        PluginRegistry registry = new PluginRegistry();
        Stream.of(plugins).forEach(registry::register);
        return registry;
    }
}