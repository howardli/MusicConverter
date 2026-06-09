package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PluginRegistry 单元测试
 */
class PluginRegistryTest {

    @Test
    @DisplayName("of - 创建并注册多个插件")
    void testOfMultiplePlugins() {
        PluginRegistry registry = PluginRegistry.of(new CopyPlugin(), new NeteasePlugin());

        assertTrue(registry.supports("ncm"));
        assertTrue(registry.supports("mp3"));
        assertTrue(registry.supports("flac"));
    }

    @Test
    @DisplayName("supports - 判断是否支持扩展名")
    void testSupports() {
        PluginRegistry registry = PluginRegistry.of(new NeteasePlugin());

        assertTrue(registry.supports("ncm"));
        assertTrue(registry.supports("NCM"));  // 大小写不敏感
        assertFalse(registry.supports("mp3"));
        assertFalse(registry.supports(null));
    }

    @Test
    @DisplayName("find - 查找插件")
    void testFind() {
        PluginRegistry registry = PluginRegistry.of(new NeteasePlugin());

        Optional<FormatPlugin> plugin = registry.find("ncm");
        assertTrue(plugin.isPresent());
        assertEquals("Netease Cloud Music", plugin.get().pluginName());

        Optional<FormatPlugin> notFound = registry.find("mp3");
        assertFalse(notFound.isPresent());

        Optional<FormatPlugin> nullExt = registry.find(null);
        assertFalse(nullExt.isPresent());
    }

    @Test
    @DisplayName("allExtensions - 获取所有支持的扩展名")
    void testAllExtensions() {
        PluginRegistry registry = PluginRegistry.of(new CopyPlugin(), new NeteasePlugin());

        Set<String> extensions = registry.allExtensions();
        assertTrue(extensions.contains("ncm"));
        assertTrue(extensions.contains("mp3"));
        assertTrue(extensions.contains("flac"));
        assertTrue(extensions.contains("wav"));
        assertTrue(extensions.contains("aac"));
        assertTrue(extensions.contains("ogg"));
        assertTrue(extensions.contains("m4a"));
    }

    @Test
    @DisplayName("register - 链式注册")
    void testRegisterChain() {
        PluginRegistry registry = new PluginRegistry()
                .register(new CopyPlugin())
                .register(new NeteasePlugin());

        assertTrue(registry.supports("ncm"));
        assertTrue(registry.supports("mp3"));
    }
}