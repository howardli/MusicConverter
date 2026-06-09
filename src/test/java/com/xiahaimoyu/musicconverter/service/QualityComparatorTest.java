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
    @DisplayName("shouldReplace - 非 audio 文件异常时返回 true")
    void testShouldReplaceWithNonAudioFile() throws Exception {
        Path newFile = tempDir.resolve("new.txt");
        Path oldFile = tempDir.resolve("old.txt");

        Files.writeString(newFile, "new");
        Files.writeString(oldFile, "old");

        // 非 audio 文件会触发异常，返回 true 并记录日志
        assertTrue(comparator.shouldReplace(newFile, oldFile));
    }
}