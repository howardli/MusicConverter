package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConversionService 单元测试
 */
class ConversionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("supportedFormats - 返回所有支持的格式")
    void testSupportedFormats() {
        ConversionService service = new ConversionService(
                PluginRegistry.of(new CopyPlugin(), new NeteasePlugin()));

        Set<String> formats = service.supportedFormats();

        assertTrue(formats.contains("ncm"));
        assertTrue(formats.contains("mp3"));
        assertTrue(formats.contains("flac"));
        assertTrue(formats.contains("wav"));
    }

    @Test
    @DisplayName("processFile - 复制普通音频文件")
    void testProcessFileCopy() throws Exception {
        ConversionService service = new ConversionService(
                PluginRegistry.of(new CopyPlugin()));

        Path source = tempDir.resolve("source/test.mp3");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "audio content");

        ConversionSummary.Result result = service.processFile(source, source.getParent(), target);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("processFile - 不支持的格式返回 skipped")
    void testProcessFileUnsupportedFormat() throws Exception {
        ConversionService service = new ConversionService(
                PluginRegistry.of(new CopyPlugin()));

        Path source = tempDir.resolve("test.unknown");
        Files.writeString(source, "content");

        ConversionSummary.Result result = service.processFile(source, tempDir, tempDir);

        assertTrue(result.isSkipped());
    }

    @Test
    @DisplayName("processDirectory - 处理整个目录")
    void testProcessDirectory() throws Exception {
        ConversionService service = new ConversionService(
                PluginRegistry.of(new CopyPlugin()));

        Path sourceDir = tempDir.resolve("source");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);

        // 创建多个文件
        Files.writeString(sourceDir.resolve("a.mp3"), "a");
        Files.writeString(sourceDir.resolve("b.flac"), "b");
        Files.writeString(sourceDir.resolve("c.unknown"), "c");

        ConversionSummary summary = service.processDirectory(sourceDir, targetDir);

        assertEquals(3, summary.totalFiles());
        assertEquals(2, summary.successCount());
        assertEquals(1, summary.skippedCount());
        assertEquals(0, summary.failedCount());
    }

    @Test
    @DisplayName("processDirectory - 空目录")
    void testProcessDirectoryEmpty() throws Exception {
        ConversionService service = new ConversionService(
                PluginRegistry.of(new CopyPlugin()));

        Path sourceDir = tempDir.resolve("empty");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);

        ConversionSummary summary = service.processDirectory(sourceDir, targetDir);

        assertEquals(0, summary.totalFiles());
        assertEquals(0, summary.successCount());
    }
}