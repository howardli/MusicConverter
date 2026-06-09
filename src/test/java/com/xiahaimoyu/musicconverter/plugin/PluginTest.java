package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 插件类单元测试
 */
class PluginTest {

    @Test
    @DisplayName("CopyPlugin - 基本信息")
    void testCopyPlugin() {
        CopyPlugin plugin = new CopyPlugin();

        assertEquals("Direct Copy", plugin.pluginName());
        assertTrue(plugin.supportedExtensions().contains("mp3"));
        assertTrue(plugin.supportedExtensions().contains("flac"));
        assertTrue(plugin.supportedExtensions().contains("wav"));
        assertTrue(plugin.supportedExtensions().contains("aac"));
        assertTrue(plugin.supportedExtensions().contains("ogg"));
        assertTrue(plugin.supportedExtensions().contains("m4a"));
        assertEquals(6, plugin.supportedExtensions().size());

        assertNotNull(plugin.createDecoder());
        assertFalse(plugin.createMetadataHandler().isPresent());
    }

    @Test
    @DisplayName("NeteasePlugin - 基本信息")
    void testNeteasePlugin() {
        NeteasePlugin plugin = new NeteasePlugin();

        assertEquals("Netease Cloud Music", plugin.pluginName());
        assertTrue(plugin.supportedExtensions().contains("ncm"));
        assertEquals(1, plugin.supportedExtensions().size());

        assertNotNull(plugin.createDecoder());
        assertTrue(plugin.createMetadataHandler().isPresent());
    }
}