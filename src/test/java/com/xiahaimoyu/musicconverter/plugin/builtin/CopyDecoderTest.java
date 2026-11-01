package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

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
        Files.write(source, "test audio content".getBytes(StandardCharsets.UTF_8));

        // 目标路径（不含扩展名）
        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.mp3"), result.outputFile());
        assertNull(result.metadata());
        assertFalse(result.hasMetadata());

        // 验证文件已复制
        assertTrue(Files.exists(result.outputFile()));
        assertEquals("test audio content", new String(Files.readAllBytes(result.outputFile()), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decode - 不同扩展名")
    void testDecodeDifferentExtension() throws Exception {
        Path source = tempDir.resolve("test.flac");
        Files.write(source, "flac content".getBytes(StandardCharsets.UTF_8));

        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.flac"), result.outputFile());
    }

    @Test
    @DisplayName("decode - 覆盖已存在的文件")
    void testDecodeOverwrite() throws Exception {
        Path source = tempDir.resolve("source.mp3");
        Files.write(source, "new content".getBytes(StandardCharsets.UTF_8));

        Path outputBase = tempDir.resolve("output");
        Path existingTarget = tempDir.resolve("output.mp3");
        Files.write(existingTarget, "old content".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals("new content", new String(Files.readAllBytes(result.outputFile()), StandardCharsets.UTF_8));
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
    @DisplayName("decode - 无扩展名的文件直接使用输出路径")
    void testDecodeNoExtension() throws Exception {
        Path source = tempDir.resolve("testfile");
        Files.write(source, "content".getBytes(StandardCharsets.UTF_8));

        Path outputBase = tempDir.resolve("output");

        ConversionResult result = decoder.decode(source, outputBase);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output"), result.outputFile());
        assertTrue(Files.exists(result.outputFile()));
    }
}
