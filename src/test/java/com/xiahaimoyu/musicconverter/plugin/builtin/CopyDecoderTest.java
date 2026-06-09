package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CopyDecoder 单元测试
 */
class CopyDecoderTest {

    private final CopyDecoder decoder = new CopyDecoder();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("decode - 成功复制文件")
    void testDecodeSuccess() throws Exception {
        // 创建源文件
        Path source = tempDir.resolve("test.mp3");
        Files.writeString(source, "test audio content");

        // 目标路径（不含扩展名）
        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.mp3"), result.outputFile());
        assertNull(result.metadata());
        assertFalse(result.hasMetadata());

        // 验证文件已复制
        assertTrue(Files.exists(result.outputFile()));
        assertEquals("test audio content", Files.readString(result.outputFile()));
    }

    @Test
    @DisplayName("decode - 不同扩展名")
    void testDecodeDifferentExtension() throws Exception {
        Path source = tempDir.resolve("test.flac");
        Files.writeString(source, "flac content");

        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.flac"), result.outputFile());
    }

    @Test
    @DisplayName("decode - 覆盖已存在的文件")
    void testDecodeOverwrite() throws Exception {
        Path source = tempDir.resolve("source.mp3");
        Files.writeString(source, "new content");

        Path outputBase = tempDir.resolve("output");
        Path existingTarget = tempDir.resolve("output.mp3");
        Files.writeString(existingTarget, "old content");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals("new content", Files.readString(result.outputFile()));
    }

    @Test
    @DisplayName("decode - 源文件不存在抛出异常")
    void testDecodeSourceNotExist() {
        Path source = tempDir.resolve("notexist.mp3");
        Path outputBase = tempDir.resolve("output");

        assertThrows(ConverterException.class, () ->
                decoder.decode(source, outputBase));
    }

    @Test
    @DisplayName("decode - 无扩展名的文件")
    void testDecodeNoExtension() throws Exception {
        Path source = tempDir.resolve("testfile");
        Files.writeString(source, "content");

        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        // 无扩展名时 extractExtension 返回 null，路径会变成 "output.null"
        assertTrue(result.isSuccess());
        assertTrue(result.outputFile().getFileName().toString().endsWith(".null"));
    }
}