package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.AudioDecoder;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NCM 解码器
 * <p>
 * 将网易云 NCM 加密格式解码为标准 MP3/FLAC
 */
public final class NcmDecoder implements AudioDecoder {

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

            // 5. 合并封面到元数据（无封面时仍继续处理）
            AudioMetadata.Builder metadataBuilder = AudioMetadata.builder()
                    .title(metadata.title())
                    .album(metadata.album())
                    .artists(metadata.artists())
                    .format(metadata.format());
            if (coverArt != null) {
                metadataBuilder.coverArt(coverArt);
            }
            AudioMetadata fullMetadata = metadataBuilder.build();

            // 6. 解密音频数据
            Path outputFile = Paths.get(output + "." + fullMetadata.format());
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