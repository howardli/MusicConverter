package com.xiahaimoyu.musicconverter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileOrganizer 单元测试
 */
class FileOrganizerTest {

    private final FileOrganizer fileOrganizer = new FileOrganizer();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ensureTargetDir - 创建目标目录")
    void testEnsureTargetDir() throws Exception {
        Path sourceRoot = tempDir.resolve("source");
        Path targetRoot = tempDir.resolve("target");
        Path sourceFile = sourceRoot.resolve("subdir/test.ncm");

        Files.createDirectories(sourceFile.getParent());

        Path targetDir = fileOrganizer.ensureTargetDir(sourceFile, sourceRoot, targetRoot);

        assertTrue(Files.exists(targetDir));
        assertEquals(targetRoot.resolve("subdir"), targetDir);
    }

    @Test
    @DisplayName("generateTempFile - 生成临时文件路径")
    void testGenerateTempFile() throws Exception {
        Path targetDir = tempDir.resolve("target");
        Path source = tempDir.resolve("source/test song.ncm");

        Files.createDirectories(targetDir);

        Path tempFile = fileOrganizer.generateTempFile(targetDir, source);

        assertTrue(tempFile.getFileName().toString().startsWith("tmp_"));
        assertTrue(tempFile.getFileName().toString().contains("test song"));
    }

    @Test
    @DisplayName("resolveFinalFile - 解析最终文件路径")
    void testResolveFinalFile() throws Exception {
        Path targetDir = tempDir.resolve("target");
        Path tempFile = targetDir.resolve("tmp_test.mp3");
        Path source = tempDir.resolve("source/test.ncm");

        Path finalFile = fileOrganizer.resolveFinalFile(targetDir, tempFile, source);

        assertEquals("test.mp3", finalFile.getFileName().toString());
    }

    @Test
    @DisplayName("cleanup - 清理临时文件")
    void testCleanup() throws Exception {
        Path tempFile = tempDir.resolve("tmp_test.mp3");
        Files.writeString(tempFile, "test content");

        assertTrue(Files.exists(tempFile));

        fileOrganizer.cleanup(tempFile);

        assertFalse(Files.exists(tempFile));
    }

    @Test
    @DisplayName("replace - 替换文件")
    void testReplace() throws Exception {
        Path newFile = tempDir.resolve("new.mp3");
        Path oldFile = tempDir.resolve("old.mp3");

        Files.writeString(newFile, "new content");
        Files.writeString(oldFile, "old content");

        fileOrganizer.replace(newFile, oldFile);

        assertFalse(Files.exists(newFile));
        assertTrue(Files.exists(oldFile));
        assertEquals("new content", Files.readString(oldFile));
    }
}