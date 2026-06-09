package com.xiahaimoyu.musicconverter.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConverterException 单元测试
 */
class ConverterExceptionTest {

    @Test
    @DisplayName("构造方法 - 无 cause")
    void testConstructorWithoutCause() {
        Path file = Paths.get("/test/file.ncm");
        ConverterException ex = new ConverterException(
                ConverterException.ErrorCode.FILE_NOT_FOUND, file);

        assertEquals(ConverterException.ErrorCode.FILE_NOT_FOUND, ex.code());
        assertEquals(file, ex.file());
        assertTrue(ex.getMessage().contains("文件不存在"));
        assertTrue(ex.getMessage().contains(file.toString()));
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("构造方法 - 有 cause")
    void testConstructorWithCause() {
        Path file = Paths.get("/test/file.ncm");
        IOException cause = new IOException("IO error");
        ConverterException ex = new ConverterException(
                ConverterException.ErrorCode.DECRYPT_FAILED, file, cause);

        assertEquals(ConverterException.ErrorCode.DECRYPT_FAILED, ex.code());
        assertEquals(file, ex.file());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("ErrorCode - 所有错误码有消息")
    void testErrorCodeMessages() {
        for (ConverterException.ErrorCode code : ConverterException.ErrorCode.values()) {
            assertNotNull(code.message());
            assertFalse(code.message().isEmpty());
        }
    }

    @Test
    @DisplayName("ErrorCode - 错误码数量")
    void testErrorCodeCount() {
        assertEquals(9, ConverterException.ErrorCode.values().length);
    }

    @Test
    @DisplayName("ErrorCode - 具体消息内容")
    void testErrorCodeSpecificMessages() {
        assertEquals("文件不存在", ConverterException.ErrorCode.FILE_NOT_FOUND.message());
        assertEquals("文件格式无效", ConverterException.ErrorCode.INVALID_FORMAT.message());
        assertEquals("解密失败", ConverterException.ErrorCode.DECRYPT_FAILED.message());
        assertEquals("元数据解析失败", ConverterException.ErrorCode.METADATA_PARSE_FAILED.message());
        assertEquals("元数据写入失败", ConverterException.ErrorCode.METADATA_WRITE_FAILED.message());
        assertEquals("文件写入失败", ConverterException.ErrorCode.FILE_WRITE_FAILED.message());
        assertEquals("音频处理失败", ConverterException.ErrorCode.AUDIO_PROCESS_FAILED.message());
        assertEquals("封面图片处理失败", ConverterException.ErrorCode.COVER_ART_FAILED.message());
        assertEquals("不支持的文件格式", ConverterException.ErrorCode.UNSUPPORTED_FORMAT.message());
    }
}