package com.xiahaimoyu.musicconverter.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConversionResult 单元测试
 */
class ConversionResultTest {

    @Test
    @DisplayName("success - 创建成功结果")
    void testSuccess() {
        Path output = Paths.get("/tmp/test.mp3");
        AudioMetadata metadata = AudioMetadata.builder()
                .title("测试歌曲")
                .build();

        ConversionResult result = ConversionResult.success(output, metadata);

        assertTrue(result.isSuccess());
        assertEquals(output, result.outputFile());
        assertEquals(metadata, result.metadata());
        assertTrue(result.hasMetadata());
    }

    @Test
    @DisplayName("success - 无元数据的成功结果")
    void testSuccessWithoutMetadata() {
        Path output = Paths.get("/tmp/test.mp3");

        ConversionResult result = ConversionResult.success(output, null);

        assertTrue(result.isSuccess());
        assertEquals(output, result.outputFile());
        assertNull(result.metadata());
        assertFalse(result.hasMetadata());
    }

    @Test
    @DisplayName("failure - 创建失败结果")
    void testFailure() {
        ConversionResult result = ConversionResult.failure();

        assertFalse(result.isSuccess());
        assertNull(result.outputFile());
        assertNull(result.metadata());
        assertFalse(result.hasMetadata());
    }

    @Test
    @DisplayName("outputFile - 返回正确路径")
    void testOutputFile() {
        Path expected = Paths.get("/music/output.mp3");
        ConversionResult result = ConversionResult.success(expected, null);

        assertEquals(expected, result.outputFile());
    }

    @Test
    @DisplayName("metadata - 返回正确元数据")
    void testMetadata() {
        AudioMetadata expected = AudioMetadata.builder()
                .title("歌曲")
                .album("专辑")
                .build();
        ConversionResult result = ConversionResult.success(Paths.get("/tmp/test.mp3"), expected);

        assertEquals(expected, result.metadata());
        assertEquals("歌曲", result.metadata().title());
        assertEquals("专辑", result.metadata().album());
    }
}