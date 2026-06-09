package com.xiahaimoyu.musicconverter.util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;

import java.io.File;

/**
 * 音频工具类
 */
public final class AudioUtils {

    private AudioUtils() {}

    /**
     * 比较两个音频文件的音质
     * <p>
     * 优先比较比特率，相同时比较采样率
     */
    public static boolean hasHigherQuality(File newFile, File oldFile) {
        try {
            AudioHeader newHeader = readHeader(newFile);
            AudioHeader oldHeader = readHeader(oldFile);

            int cmp = Long.compare(newHeader.getBitRateAsNumber(), oldHeader.getBitRateAsNumber());
            if (cmp != 0) return cmp > 0;

            return newHeader.getSampleRateAsNumber() > oldHeader.getSampleRateAsNumber();
        } catch (Exception e) {
            return false;
        }
    }

    private static AudioHeader readHeader(File file) throws Exception {
        return AudioFileIO.read(file).getAudioHeader();
    }
}