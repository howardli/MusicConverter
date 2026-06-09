package com.xiahaimoyu.musicconverter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageUtils 单元测试
 */
class ImageUtilsTest {

    // JPEG 文件头: FF D8 FF
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // PNG 文件头: 89 50 4E 47
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    // GIF 文件头: GIF
    private static final byte[] GIF_HEADER = {'G', 'I', 'F', 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // WebP 文件头: RIFF....WEBP
    private static final byte[] WEBP_HEADER = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    // BMP 文件头: BM
    private static final byte[] BMP_HEADER = {'B', 'M', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Test
    @DisplayName("detectMimeType - JPEG")
    void testDetectMimeTypeJpeg() {
        Optional<String> mime = ImageUtils.detectMimeType(JPEG_HEADER);
        assertTrue(mime.isPresent());
        assertEquals("image/jpeg", mime.get());
    }

    @Test
    @DisplayName("detectMimeType - PNG")
    void testDetectMimeTypePng() {
        Optional<String> mime = ImageUtils.detectMimeType(PNG_HEADER);
        assertTrue(mime.isPresent());
        assertEquals("image/png", mime.get());
    }

    @Test
    @DisplayName("detectMimeType - GIF")
    void testDetectMimeTypeGif() {
        Optional<String> mime = ImageUtils.detectMimeType(GIF_HEADER);
        assertTrue(mime.isPresent());
        assertEquals("image/gif", mime.get());
    }

    @Test
    @DisplayName("detectMimeType - WebP")
    void testDetectMimeTypeWebp() {
        Optional<String> mime = ImageUtils.detectMimeType(WEBP_HEADER);
        assertTrue(mime.isPresent());
        assertEquals("image/webp", mime.get());
    }

    @Test
    @DisplayName("detectMimeType - BMP")
    void testDetectMimeTypeBmp() {
        Optional<String> mime = ImageUtils.detectMimeType(BMP_HEADER);
        assertTrue(mime.isPresent());
        assertEquals("image/bmp", mime.get());
    }

    @Test
    @DisplayName("detectMimeType - 未知格式返回 empty")
    void testDetectMimeTypeUnknown() {
        byte[] unknown = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Optional<String> mime = ImageUtils.detectMimeType(unknown);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - null 返回 empty")
    void testDetectMimeTypeNull() {
        Optional<String> mime = ImageUtils.detectMimeType(null);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - 空数组返回 empty")
    void testDetectMimeTypeEmpty() {
        Optional<String> mime = ImageUtils.detectMimeType(new byte[0]);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - 短数组返回 empty")
    void testDetectMimeTypeShort() {
        Optional<String> mime = ImageUtils.detectMimeType(new byte[5]);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("resize - null 返回 empty")
    void testResizeNull() {
        Optional<byte[]> result = ImageUtils.resize(null, 800, 800);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("resize - 空数组返回 empty")
    void testResizeEmpty() {
        Optional<byte[]> result = ImageUtils.resize(new byte[0], 800, 800);
        assertFalse(result.isPresent());
    }
}