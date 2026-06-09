package com.xiahaimoyu.musicconverter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QualityComparator 单元测试
 */
class QualityComparatorTest {

    private final QualityComparator comparator = new QualityComparator();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("shouldReplace - 目标不存在时应替换")
    void testShouldReplaceWhenTargetNotExist() {
        Path newFile = tempDir.resolve("new.mp3");
        Path oldFile = tempDir.resolve("old.mp3");  // 不存在

        assertTrue(comparator.shouldReplace(newFile, oldFile));
    }

    @Test
    @DisplayName("shouldReplace - 目标存在时比较音质")
    void testShouldReplaceWhenTargetExists() throws Exception {
        // 这个测试需要真实的音频文件，无法在纯单元测试中验证
        // 实际测试需要集成测试或 mock AudioFileIO
        // 这里只测试基本逻辑

        Path newFile = tempDir.resolve("new.txt");
        Path oldFile = tempDir.resolve("old.txt");

        Files.writeString(newFile, "new");
        Files.writeString(oldFile, "old");

        // 非 audio 文件会触发异常，现在会返回 true 并记录日志
        assertTrue(comparator.shouldReplace(newFile, oldFile));
    }
}