package com.xiahaimoyu.musicconverter.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AudioMetadata 单元测试
 */
class AudioMetadataTest {

    @Test
    @DisplayName("Builder - 构建完整元数据")
    void testBuilderFull() {
        byte[] cover = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        AudioMetadata metadata = AudioMetadata.builder()
                .title("测试歌曲")
                .album("测试专辑")
                .artists(Arrays.asList("歌手A", "歌手B"))
                .coverArt(cover)
                .coverMimeType("image/jpeg")
                .format("mp3")
                .lyrics("歌词内容")
                .build();

        assertEquals("测试歌曲", metadata.title());
        assertEquals("测试专辑", metadata.album());
        assertEquals(2, metadata.artists().size());
        assertEquals("歌手A", metadata.primaryArtist());
        assertEquals("歌手A, 歌手B", metadata.artistDisplay());
        assertArrayEquals(cover, metadata.coverArt());
        assertEquals("image/jpeg", metadata.coverMimeType());
        assertEquals("mp3", metadata.format());
        assertEquals("歌词内容", metadata.lyrics());
        assertTrue(metadata.hasCoverArt());
    }

    @Test
    @DisplayName("Builder - 构建空元数据")
    void testBuilderEmpty() {
        AudioMetadata metadata = AudioMetadata.builder().build();

        assertEquals("", metadata.title());
        assertEquals("", metadata.album());
        assertTrue(metadata.artists().isEmpty());
        assertNull(metadata.coverArt());
        assertNull(metadata.coverMimeType());
        assertEquals("", metadata.format());
        assertNull(metadata.lyrics());
        assertFalse(metadata.hasCoverArt());
        assertEquals("", metadata.primaryArtist());
        assertEquals("", metadata.artistDisplay());
    }

    @Test
    @DisplayName("Builder - 使用数组设置艺术家")
    void testBuilderArtistsArray() {
        AudioMetadata metadata = AudioMetadata.builder()
                .artists(new String[]{"歌手A", "歌手B"})
                .build();

        assertEquals(2, metadata.artists().size());
        assertEquals("歌手A", metadata.primaryArtist());
    }

    @Test
    @DisplayName("coverArt - 返回克隆数组")
    void testCoverArtClone() {
        byte[] original = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        AudioMetadata metadata = AudioMetadata.builder()
                .coverArt(original)
                .build();

        byte[] returned = metadata.coverArt();
        assertArrayEquals(original, returned);

        // 修改返回值不影响原始
        returned[0] = 0x00;
        assertArrayEquals(original, metadata.coverArt());
    }

    @Test
    @DisplayName("artists - 返回不可修改列表")
    void testArtistsUnmodifiable() {
        AudioMetadata metadata = AudioMetadata.builder()
                .artists(Arrays.asList("歌手A", "歌手B"))
                .build();

        List<String> artists = metadata.artists();
        assertThrows(UnsupportedOperationException.class, () ->
                artists.add("歌手C"));
    }

    @Test
    @DisplayName("title/album/format - null 时返回空字符串")
    void testNullReturnsEmpty() {
        AudioMetadata metadata = AudioMetadata.builder().build();

        assertEquals("", metadata.title());
        assertEquals("", metadata.album());
        assertEquals("", metadata.format());
    }
}