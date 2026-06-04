package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.Decoder;
import com.xiahaimoyu.musicconverter.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 直接复制解码器
 * <p>
 * 将普通音频文件直接复制到目标位置
 */
public final class CopyDecoder implements Decoder {

    @Override
    public ConversionResult decode(Path source, Path output) throws ConverterException {
        String extension = PathUtils.extractExtension(source.getFileName().toString());
        Path target = Paths.get(output + "." + extension);

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, target, e);
        }

        // 直接复制的文件无元数据（从加密文件中提取）
        return ConversionResult.success(target, null);
    }
}