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
    private static final byte[] JPEG_HEADER = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    // PNG 文件头: 89 50 4E 47
    private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0x0D, 0x0A, (byte) 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};

    // GIF 文件头: GIF
    private static final byte[] GIF_HEADER = new byte[]{(byte) 'G', (byte) 'I', (byte) 'F', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    // WebP 文件头: RIFF....WEBP
    private static final byte[] WEBP_HEADER = new byte[]{
            (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F',
            0x00, 0x00, 0x00, 0x00,
            (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P'
    };

    // BMP 文件头: BM
    private static final byte[] BMP_HEADER = new byte[]{(byte) 'B', (byte) 'M', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    // 未知格式
    private static final byte[] UNKNOWN_DATA = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    // 空数据
    private static final byte[] EMPTY_DATA = new byte[]{};

    // 短数据
    private static final byte[] SHORT_DATA = new byte[]{0x00, 0x00, 0x00};

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
    @DisplayName("detectMimeType - 未知格式")
    void testDetectMimeTypeUnknown() {
        Optional<String> mime = ImageUtils.detectMimeType(UNKNOWN_DATA);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - 空数据")
    void testDetectMimeTypeEmpty() {
        Optional<String> mime = ImageUtils.detectMimeType(EMPTY_DATA);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - 短数据")
    void testDetectMimeTypeShort() {
        Optional<String> mime = ImageUtils.detectMimeType(SHORT_DATA);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("detectMimeType - null")
    void testDetectMimeTypeNull() {
        Optional<String> mime = ImageUtils.detectMimeType(null);
        assertFalse(mime.isPresent());
    }

    @Test
    @DisplayName("resize - null 数据")
    void testResizeNull() {
        Optional<byte[]> result = ImageUtils.resize(null, 800, 800);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("resize - 空数据")
    void testResizeEmpty() {
        Optional<byte[]> result = ImageUtils.resize(EMPTY_DATA, 800, 800);
        assertFalse(result.isPresent());
    }
}