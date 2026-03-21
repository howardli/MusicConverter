package com.xiahaimoyu.musicconverter.convertor;

import com.xiahaimoyu.musicconverter.util.AudioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.xiahaimoyu.musicconverter.util.PathUtils.*;

/**
 * 转换器抽象模板，提供通用转换流程
 */
public abstract class AbstractConverter implements MusicConverter {

    private static final Logger LOG = Logger.getLogger(AbstractConverter.class.getName());

    static {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    private static final String TEMP_FILE_PREFIX = "tmp_";

    @Override
    public ConversionContext convert(Path source, Path targetDir) throws Exception {
        String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
        Path tempFile = targetDir.resolve(TEMP_FILE_PREFIX + baseName);

        ConversionContext context = doConvert(source, tempFile);
        if (context == null || !Files.exists(context.tempFile())) {
            return null;
        }
        return context;
    }

    /**
     * 执行实际的转换逻辑
     *
     * @param source   源文件
     * @param tempFile 临时文件（已包含前缀和基本名，子类需添加扩展名）
     */
    protected abstract ConversionContext doConvert(Path source, Path tempFile) throws Exception;

    /**
     * 处理单个音乐文件
     */
    public void process(Path source, Path sourceRoot, Path targetRoot) {
        try {
            Path relativeDir = sourceRoot.relativize(source).getParent();
            Path targetDir = targetRoot.resolve(relativeDir != null ? relativeDir : Paths.get(""));
            Files.createDirectories(targetDir);

            ConversionContext context = convert(source, targetDir);
            if (context == null || !context.isValid()) {
                return;
            }

            Path tempFile = context.tempFile();
            String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
            Path finalFile = resolveFinalFile(targetDir, baseName, tempFile);

            if (shouldReplace(tempFile, finalFile)) {
                postProcess(context);
                replaceFile(tempFile, finalFile);
            } else {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "转换失败: " + source, e);
        }
    }

    private Path resolveFinalFile(Path targetDir, String baseName, Path tempFile) {
        return targetDir.resolve(baseName + extensionOf(tempFile.getFileName().toString()));
    }

    private boolean shouldReplace(Path newFile, Path oldFile) throws IOException {
        if (!Files.exists(oldFile)) {
            return true;
        }
        return AudioUtils.hasHigherQuality(newFile.toFile(), oldFile.toFile());
    }

    private void replaceFile(Path tempFile, Path finalFile) throws IOException {
        Files.deleteIfExists(finalFile);
        Files.move(tempFile, finalFile);
    }
}