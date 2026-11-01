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

    @Test
    @DisplayName("touch - 刷新文件修改时间")
    void testTouch() throws Exception {
        Path file = tempDir.resolve("song.flac");
        Files.write(file, new byte[]{1});
        long oldTime = System.currentTimeMillis() - 60_000;
        Files.setLastModifiedTime(file,
                java.nio.file.attribute.FileTime.fromMillis(oldTime));

        fileOrganizer.touch(file);

        assertTrue(Files.getLastModifiedTime(file).toMillis() > oldTime + 30_000);
    }

    @Test
    @DisplayName("hasFreshOutput - NCM 已有不早于源文件的产物时跳过")
    void testHasFreshOutputNcmFresh() throws Exception {
        Path source = tempDir.resolve("song.ncm");
        Path targetDir = tempDir.resolve("target");
        Files.write(source, new byte[]{1});
        Files.createDirectories(targetDir);

        Path output = targetDir.resolve("song.flac");
        Files.write(output, new byte[]{1});
        Files.setLastModifiedTime(output, Files.getLastModifiedTime(source));

        assertTrue(fileOrganizer.hasFreshOutput(source, targetDir,
                java.util.Arrays.asList(".flac", ".mp3")));
    }

    @Test
    @DisplayName("hasFreshOutput - 产物早于源文件（源已更新）时不跳过")
    void testHasFreshOutputStale() throws Exception {
        Path source = tempDir.resolve("song.ncm");
        Path targetDir = tempDir.resolve("target");
        Files.write(source, new byte[]{1});
        Files.createDirectories(targetDir);

        Path output = targetDir.resolve("song.flac");
        Files.write(output, new byte[]{1});
        Files.setLastModifiedTime(output,
                java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 60_000));

        assertFalse(fileOrganizer.hasFreshOutput(source, targetDir,
                java.util.Arrays.asList(".flac", ".mp3")));
    }

    @Test
    @DisplayName("hasFreshOutput - 无产物时不跳过")
    void testHasFreshOutputMissing() throws Exception {
        Path source = tempDir.resolve("song.ncm");
        Path targetDir = tempDir.resolve("target");
        Files.write(source, new byte[]{1});
        Files.createDirectories(targetDir);

        assertFalse(fileOrganizer.hasFreshOutput(source, targetDir,
                java.util.Arrays.asList(".flac", ".mp3")));
    }

    @Test
    @DisplayName("hasFreshOutput - 普通格式按原扩展名探测")
    void testHasFreshOutputCopyFormat() throws Exception {
        Path source = tempDir.resolve("song.mp3");
        Path targetDir = tempDir.resolve("target");
        Files.write(source, new byte[]{1});
        Files.createDirectories(targetDir);

        Path output = targetDir.resolve("song.mp3");
        Files.write(output, new byte[]{1});
        Files.setLastModifiedTime(output, Files.getLastModifiedTime(source));

        assertTrue(fileOrganizer.hasFreshOutput(source, targetDir,
                java.util.Arrays.asList(".mp3")));
        assertFalse(fileOrganizer.hasFreshOutput(source, targetDir,
                java.util.Arrays.asList(".flac")));
    }
}