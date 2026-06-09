package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;
import com.xiahaimoyu.musicconverter.util.ImageUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.nio.file.Path;

/**
 * NCM 元数据处理器
 * <p>
 * 将提取的元数据（标题、专辑、艺术家、封面）写入音频文件
 */
public final class NcmMetadataHandler implements MetadataHandler {

    @Override
    public void writeMetadata(Path file, AudioMetadata metadata) throws ConverterException {
        try {
            AudioFile audio = AudioFileIO.read(file.toFile());
            Tag tag = audio.getTagOrCreateAndSetDefault();

            // 写入基本信息
            tag.setField(FieldKey.TITLE, metadata.title());
            tag.setField(FieldKey.ALBUM, metadata.album());
            tag.setField(FieldKey.ARTIST, metadata.primaryArtist());

            // 写入封面
            if (metadata.hasCoverArt()) {
                writeCoverArt(tag, metadata);
            }

            audio.commit();
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.METADATA_WRITE_FAILED, file, e);
        }
    }

    private void writeCoverArt(Tag tag, AudioMetadata metadata) throws Exception {
        byte[] coverArt = metadata.coverArt();

        // 缩放封面图片
        byte[] resized = ImageUtils.resize(coverArt,
                ConverterConfig.MAX_COVER_WIDTH,
                ConverterConfig.MAX_COVER_HEIGHT)
                .orElse(coverArt);

        // 检测 MIME 类型并写入
        ImageUtils.detectMimeType(resized).ifPresent(mime -> {
            try {
                tag.deleteArtworkField();
                StandardArtwork artwork = new StandardArtwork();
                artwork.setBinaryData(resized);
                artwork.setMimeType(mime);
                tag.setField(artwork);
            } catch (Exception ignored) {
                // 封面写入失败不影响主流程
            }
        });
    }
}