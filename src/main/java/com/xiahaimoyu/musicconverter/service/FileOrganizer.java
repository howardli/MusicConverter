package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.xiahaimoyu.musicconverter.util.PathUtils.baseNameOf;
import static com.xiahaimoyu.musicconverter.util.PathUtils.extensionOf;
import static com.xiahaimoyu.musicconverter.util.PathUtils.normalize;
import static com.xiahaimoyu.musicconverter.util.PathUtils.sanitize;

/**
 * 文件组织服务
 * <p>
 * 负责目标目录创建、文件命名、临时文件清理
 */
public final class FileOrganizer {

    /**
     * 确保目标目录存在
     *
     * @param source     源文件路径
     * @param sourceRoot 源目录根
     * @param targetRoot 目标目录根
     * @return 目标目录路径
     * @throws ConverterException 创建目录失败
     */
    public Path ensureTargetDir(Path source, Path sourceRoot, Path targetRoot) {
        Path relativeDir = sourceRoot.relativize(source).getParent();
        Path targetDir = targetRoot.resolve(relativeDir != null ? relativeDir : Path.of(""));
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, targetDir, e);
        }
        return targetDir;
    }

    /**
     * 生成临时文件路径
     *
     * @param targetDir 目标目录
     * @param source    源文件（用于提取基本名）
     * @return 临时文件路径（不含扩展名）
     */
    public Path generateTempFile(Path targetDir, Path source) {
        String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
        return targetDir.resolve(ConverterConfig.TEMP_FILE_PREFIX + baseName);
    }

    /**
     * 生成最终文件路径
     *
     * @param targetDir 目标目录
     * @param tempFile  临时文件（用于提取扩展名）
     * @param source    源文件（用于提取基本名）
     * @return 最终文件路径
     */
    public Path resolveFinalFile(Path targetDir, Path tempFile, Path source) {
        String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
        String extension = extensionOf(tempFile.getFileName().toString());
        return targetDir.resolve(baseName + extension);
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件路径
     */
    public void cleanup(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // 清理失败不影响主流程
        }
    }

    /**
     * 替换文件（删除旧文件，移动新文件）
     *
     * @param newFile 新文件
     * @param oldFile 旧文件
     * @throws ConverterException 操作失败
     */
    public void replace(Path newFile, Path oldFile) throws ConverterException {
        try {
            Files.deleteIfExists(oldFile);
            Files.move(newFile, oldFile);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, oldFile, e);
        }
    }
}