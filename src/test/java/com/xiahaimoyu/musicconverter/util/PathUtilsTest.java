package com.xiahaimoyu.musicconverter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PathUtils 单元测试
 */
class PathUtilsTest {

    @Test
    @DisplayName("normalizeToNFC - NFD 转 NFC")
    void testNormalizeToNFC() {
        // NFD 形式（分解形式）
        String nfd = "café";  // cafe + combining acute accent
        // NFC 形式（组合形式）
        String nfc = PathUtils.normalizeToNFC(nfd);
        assertEquals("café", nfc);  // café
    }

    @Test
    @DisplayName("extractBaseName - 提取基本名")
    void testExtractBaseName() {
        assertEquals("test", PathUtils.extractBaseName("test.mp3"));
        assertEquals("test.flac", PathUtils.extractBaseName("test.flac.ncm"));
        assertEquals("noextension", PathUtils.extractBaseName("noextension"));
        assertEquals(".hidden", PathUtils.extractBaseName(".hidden"));  // 点号在首位，无基本名
    }

    @Test
    @DisplayName("extensionOf - 获取扩展名（含点号）")
    void testExtensionOf() {
        assertEquals(".mp3", PathUtils.extensionOf("test.mp3"));
        assertEquals(".ncm", PathUtils.extensionOf("test.flac.ncm"));
        assertEquals("", PathUtils.extensionOf("noextension"));
    }

    @Test
    @DisplayName("replaceIllegalChars - 替换非法字符")
    void testReplaceIllegalChars() {
        assertEquals("test_file", PathUtils.replaceIllegalChars("test\\file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test:file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test*file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test?file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test\"file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test<file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test>file"));
        assertEquals("test_file", PathUtils.replaceIllegalChars("test|file"));
        assertEquals("normal_file_name", PathUtils.replaceIllegalChars("normal_file_name"));
    }

    @Test
    @DisplayName("extractExtension - 提取扩展名（小写，无点号）")
    void testExtractExtension() {
        assertEquals("mp3", PathUtils.extractExtension("test.MP3"));
        assertEquals("ncm", PathUtils.extractExtension("test.ncm"));
        assertNull(PathUtils.extractExtension("noextension"));
        assertNull(PathUtils.extractExtension(".hidden"));
    }
}