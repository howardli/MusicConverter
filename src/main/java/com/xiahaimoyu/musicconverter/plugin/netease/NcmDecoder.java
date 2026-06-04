package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.Decoder;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * NCM 解码器
 * <p>
 * 将网易云 NCM 加密格式解码为标准 MP3/FLAC
 */
public final class NcmDecoder implements Decoder {

    @Override
    public ConversionResult decode(Path source, Path output) throws ConverterException {
        try (RandomAccessFile raf = openFile(source)) {
            // 1. 验证格式
            if (!NcmFormat.isValidFormat(raf)) {
                throw new ConverterException(ErrorCode.INVALID_FORMAT, source);
            }

            // 2. 读取密钥
            int[] sbox = NcmFormat.readKeyAndBuildSbox(raf);

            // 3. 读取元数据
            AudioMetadata metadata = NcmFormat.readMetadata(raf);

            // 4. 读取封面
            byte[] coverArt = NcmFormat.readCoverArt(raf);
            if (coverArt == null) {
                return ConversionResult.failure();
            }

            // 5. 合并封面到元数据
            AudioMetadata fullMetadata = AudioMetadata.builder()
                    .title(metadata.title())
                    .album(metadata.album())
                    .artists(metadata.artists())
                    .format(metadata.format())
                    .coverArt(coverArt)
                    .build();

            // 6. 解密音频数据
            Path outputFile = Path.of(output + "." + fullMetadata.format());
            NcmFormat.decryptAudio(raf, sbox, outputFile);

            return ConversionResult.success(outputFile, fullMetadata);
        } catch (ConverterException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw new ConverterException(ErrorCode.FILE_NOT_FOUND, source, e);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.DECRYPT_FAILED, source, e);
        }
    }

    private RandomAccessFile openFile(Path source) throws FileNotFoundException {
        return new RandomAccessFile(source.toFile(), "r");
    }
}