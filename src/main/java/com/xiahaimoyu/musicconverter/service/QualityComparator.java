package com.xiahaimoyu.musicconverter.service;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 音质比较服务
 * <p>
 * 比较两个音频文件的音质，决定是否需要替换
 */
public final class QualityComparator {

    private static final Logger LOG = Logger.getLogger(QualityComparator.class.getName());

    /**
     * 判断新文件是否应替换旧文件
     * <p>
     * 旧文件不存在时返回 true；存在时比较音质
     *
     * @param newFile 新文件路径
     * @param oldFile 旧文件路径
     * @return 是否应替换
     */
    public boolean shouldReplace(Path newFile, Path oldFile) {
        if (!Files.exists(oldFile)) {
            return true;
        }
        return hasHigherQuality(newFile, oldFile);
    }

    /**
     * 判断新文件音质是否高于旧文件
     * <p>
     * 优先比较比特率，相同则比较采样率
     */
    private boolean hasHigherQuality(Path newFile, Path oldFile) {
        try {
            AudioHeader newHeader = readHeader(newFile);
            AudioHeader oldHeader = readHeader(oldFile);

            int bitrateCompare = Long.compare(
                    newHeader.getBitRateAsNumber(),
                    oldHeader.getBitRateAsNumber());

            if (bitrateCompare != 0) {
                return bitrateCompare > 0;
            }

            return newHeader.getSampleRateAsNumber() > oldHeader.getSampleRateAsNumber();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "无法比较音质，默认保留新文件: " + newFile, e);
            return true;  // 无法比较时，保留新文件避免丢失更高音质
        }
    }

    private AudioHeader readHeader(Path file) throws Exception {
        AudioFile audio = AudioFileIO.read(file.toFile());
        return audio.getAudioHeader();
    }
}