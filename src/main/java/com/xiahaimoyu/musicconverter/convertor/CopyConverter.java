package com.xiahaimoyu.musicconverter.convertor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.xiahaimoyu.musicconverter.util.PathUtils.extensionOf;

/**
 * 音频文件复制器
 * <p>
 * 用于直接复制已解密的音频文件（MP3、FLAC 等）
 */
public final class CopyConverter extends AbstractConverter {

    private static final Set<String> EXTENSIONS;

    static {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, "mp3", "flac", "wav", "aac", "ogg", "m4a");
        EXTENSIONS = Collections.unmodifiableSet(set);
    }

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected ConversionContext doConvert(Path source, Path tempFile) throws Exception {
        Path target = Paths.get(tempFile.toString() + extensionOf(source.getFileName().toString()));
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return ConversionContext.of(target);
    }
}