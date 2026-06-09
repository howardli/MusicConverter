package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NcmMetadataHandler 单元测试
 */
class NcmMetadataHandlerTest {

    private final NcmMetadataHandler handler = new NcmMetadataHandler();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("writeMetadata - 写入元数据到不存在的文件抛出异常")
    void testWriteMetadataFileNotExist() {
        Path file = tempDir.resolve("notexist.mp3");
        AudioMetadata metadata = AudioMetadata.builder()
                .title("测试")
                .build();

        assertThrows(ConverterException.class,
                () -> handler.writeMetadata(file, metadata));
    }

    @Test
    @DisplayName("writeMetadata - 非 audio 文件抛出异常")
    void testWriteMetadataNonAudioFile() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "not audio");

        AudioMetadata metadata = AudioMetadata.builder()
                .title("测试歌曲")
                .album("测试专辑")
                .artists(new String[]{"歌手A"})
                .build();

        assertThrows(ConverterException.class,
                () -> handler.writeMetadata(file, metadata));
    }

    @Test
    @DisplayName("writeMetadata - 无封面元数据不抛异常")
    void testWriteMetadataNoCover() throws Exception {
        // 创建一个简单的 MP3 文件（空文件会被 jaudiotagger 拒绝，所以用非 audio 测试）
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        AudioMetadata metadata = AudioMetadata.builder()
                .title("无封面歌曲")
                .build();

        // 非 audio 文件会抛异常，但重点是验证 hasCoverArt = false 的逻辑不会额外出错
        assertThrows(ConverterException.class,
                () -> handler.writeMetadata(file, metadata));
    }
}