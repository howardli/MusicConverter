package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NcmDecoder 单元测试
 * <p>
 * 测试边界情况：文件不存在、格式无效等
 */
class NcmDecoderTest {

    private final NcmDecoder decoder = new NcmDecoder();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("decode - 文件不存在抛出 FILE_NOT_FOUND")
    void testDecodeFileNotFound() {
        Path source = tempDir.resolve("nonexistent.ncm");
        Path output = tempDir.resolve("output");

        ConverterException ex = assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
        assertEquals(ErrorCode.FILE_NOT_FOUND, ex.code());
    }

    @Test
    @DisplayName("decode - 非 NCM 格式文件抛出 INVALID_FORMAT")
    void testDecodeInvalidFormat() throws Exception {
        // 创建一个不是 NCM 格式的文件
        Path source = tempDir.resolve("fake.ncm");
        Files.writeString(source, "this is not a ncm file");

        Path output = tempDir.resolve("output");

        ConverterException ex = assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
        assertEquals(ErrorCode.INVALID_FORMAT, ex.code());
    }

    @Test
    @DisplayName("decode - 空文件抛出异常")
    void testDecodeEmptyFile() throws Exception {
        Path source = tempDir.resolve("empty.ncm");
        Files.writeString(source, "");

        Path output = tempDir.resolve("output");

        assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
    }
}