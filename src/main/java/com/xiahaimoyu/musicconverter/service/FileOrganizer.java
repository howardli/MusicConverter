package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.xiahaimoyu.musicconverter.util.PathUtils.extractBaseName;
import static com.xiahaimoyu.musicconverter.util.PathUtils.extensionOf;
import static com.xiahaimoyu.musicconverter.util.PathUtils.normalizeToNFC;
import static com.xiahaimoyu.musicconverter.util.PathUtils.replaceIllegalChars;

/**
 * 文件组织服务
 * <p>
 * 负责目标目录创建、文件命名、临时文件清理
 */
public final class FileOrganizer {

    private static final Logger LOG = Logger.getLogger(FileOrganizer.class.getName());

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
        Path targetDir = targetRoot.resolve(relativeDir != null ? relativeDir : Paths.get(""));
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
        String baseName = replaceIllegalChars(extractBaseName(normalizeToNFC(source.getFileName().toString())));
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
        String baseName = replaceIllegalChars(extractBaseName(normalizeToNFC(source.getFileName().toString())));
        String extension = extensionOf(tempFile.getFileName().toString());
        return targetDir.resolve(baseName + extension);
    }

    /**
     * 判断源文件是否已存在处理产物（增量跳过用）
     * <p>
     * 目标目录中存在基于同一源文件命名的产物，且其修改时间不早于源文件，
     * 说明此前已处理过且源文件未变化，无需重复解码。
     * NCM 解码产物的扩展名在解码前未知，由调用方传入候选扩展名逐个探测。
     *
     * @param source      源文件
     * @param targetDir   目标目录
     * @param outputExts  产物可能的扩展名（含点号）
     * @return 存在新鲜产物时返回 true
     */
    public boolean hasFreshOutput(Path source, Path targetDir, List<String> outputExts) {
        String baseName = replaceIllegalChars(extractBaseName(normalizeToNFC(source.getFileName().toString())));
        try {
            FileTime sourceMtime = Files.getLastModifiedTime(source);
            for (String ext : outputExts) {
                Path finalFile = targetDir.resolve(baseName + ext);
                if (Files.exists(finalFile)
                        && Files.getLastModifiedTime(finalFile).compareTo(sourceMtime) >= 0) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "检查已处理产物失败: " + source, e);
        }
        return false;
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件路径
     */
    public void cleanup(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "临时文件清理失败: " + tempFile, e);
        }
    }

    /**
     * 原子替换：将新文件移动到旧文件位置，覆盖旧文件
     *
     * @param newFile 新文件
     * @param oldFile 旧文件
     * @throws ConverterException 操作失败
     */
    public void replace(Path newFile, Path oldFile) throws ConverterException {
        try {
            Files.move(newFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, oldFile, e);
        }
    }
}
