package com.xiahaimoyu.musicconverter.plugin.netease;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NcmFormat 单元测试
 * <p>
 * 测试包级私有的纯算法方法
 */
class NcmFormatTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("buildSbox - 生成 RC4 S-Box，应为 0-255 的排列")
    void testBuildSboxPermutation() {
        byte[] key = "testkey".getBytes();
        int[] sbox = NcmFormat.buildSbox(key);

        assertEquals(256, sbox.length);
        boolean[] seen = new boolean[256];
        for (int v : sbox) {
            assertFalse(seen[v]);
            seen[v] = true;
        }
    }

    @Test
    @DisplayName("buildSbox - 不同密钥产生不同 S-Box")
    void testBuildSboxDifferentKeys() {
        int[] sbox1 = NcmFormat.buildSbox("key1".getBytes());
        int[] sbox2 = NcmFormat.buildSbox("key2".getBytes());

        boolean differ = false;
        for (int i = 0; i < 256; i++) {
            if (sbox1[i] != sbox2[i]) { differ = true; break; }
        }
        assertTrue(differ);
    }

    @Test
    @DisplayName("xor - XOR 后数据变化，再次 XOR 恢复原始")
    void testXorRoundtrip() {
        byte[] data = {0x01, 0x64, (byte) 0xFF, 0x00};
        byte[] original = data.clone();

        NcmFormat.xor(data, 0x64);
        // 验证至少有一个字节变化
        boolean changed = false;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != original[i]) { changed = true; break; }
        }
        assertTrue(changed);

        // 再次 XOR 恢复原始
        NcmFormat.xor(data, 0x64);
        assertArrayEquals(original, data);
    }

    @Test
    @DisplayName("extractArtists - 从 JSON 数组提取艺术家")
    void testExtractArtists() {
        JSONArray arr = JSON.parseArray("[[0,\"Artist A\"],[0,\"Artist B\"]]");
        List<String> artists = NcmFormat.extractArtists(arr);

        assertEquals(2, artists.size());
        assertEquals("Artist A", artists.get(0));
        assertEquals("Artist B", artists.get(1));
    }

    @Test
    @DisplayName("extractArtists - 空数组")
    void testExtractArtistsEmptyArray() {
        JSONArray arr = JSON.parseArray("[]");
        List<String> artists = NcmFormat.extractArtists(arr);

        assertTrue(artists.isEmpty());
    }

    @Test
    @DisplayName("extractArtists - null 输入")
    void testExtractArtistsNull() {
        List<String> artists = NcmFormat.extractArtists(null);
        assertTrue(artists.isEmpty());
    }

    @Test
    @DisplayName("parseMetadata - 从 JSON 解析元数据")
    void testParseMetadata() {
        String json = "{\"musicName\":\"Test Song\",\"album\":\"Test Album\",\"format\":\"mp3\",\"artist\":[[0,\"Singer\"]]}";
        AudioMetadata metadata = NcmFormat.parseMetadata(JSON.parseObject(json));

        assertEquals("Test Song", metadata.title());
        assertEquals("Test Album", metadata.album());
        assertEquals("mp3", metadata.format());
        assertEquals("Singer", metadata.primaryArtist());
    }

    @Test
    @DisplayName("parseMetadata - 无艺术家")
    void testParseMetadataNoArtists() {
        String json = "{\"musicName\":\"Song\",\"album\":\"Album\",\"format\":\"flac\"}";
        AudioMetadata metadata = NcmFormat.parseMetadata(JSON.parseObject(json));

        assertEquals("Song", metadata.title());
        assertTrue(metadata.artists().isEmpty());
    }

    @Test
    @DisplayName("decryptAudio - 密钥流异或可完整还原明文音频")
    void testDecryptAudioRoundtrip() throws Exception {
        byte[] plain = new byte[1000];
        new Random(42).nextBytes(plain);
        int[] sbox = NcmFormat.buildSbox("testkey".getBytes());
        byte[] keystream = NcmFormat.buildKeystream(sbox);

        byte[] encrypted = plain.clone();
        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] ^= keystream[i & 0xFF];
        }
        Path source = tempDir.resolve("audio.enc");
        Files.write(source, encrypted);

        Path output = tempDir.resolve("decoded.bin");
        try (RandomAccessFile raf = new RandomAccessFile(source.toFile(), "r")) {
            NcmFormat.decryptAudio(raf, sbox, output);
        }

        assertArrayEquals(plain, Files.readAllBytes(output));
    }

    @Test
    @DisplayName("decryptAudio - 中途读取失败时清理不完整的输出文件")
    void testDecryptAudioCleansPartialOutput() throws Exception {
        int[] sbox = NcmFormat.buildSbox("testkey".getBytes());
        Path source = tempDir.resolve("audio.enc");
        Files.write(source, new byte[100]);
        Path output = tempDir.resolve("partial.out");

        RandomAccessFile raf = new RandomAccessFile(source.toFile(), "r") {
            private boolean failed;

            @Override
            public int read(byte[] b) throws IOException {
                if (failed) {
                    throw new IOException("模拟读取中断");
                }
                failed = true;
                return super.read(b);
            }
        };

        try {
            ConverterException ex = assertThrows(ConverterException.class,
                    () -> NcmFormat.decryptAudio(raf, sbox, output));
            assertEquals(ErrorCode.FILE_WRITE_FAILED, ex.code());
            assertFalse(Files.exists(output));
        } finally {
            raf.close();
        }
    }

    @Test
    @DisplayName("detectAudioFormat - 识别 fLaC 音频头")
    void testDetectAudioFormatFlac() throws Exception {
        assertEquals("flac", detect(new byte[] {'f', 'L', 'a', 'C'}));
    }

    @Test
    @DisplayName("detectAudioFormat - ID3 与 MPEG 帧同步按 mp3 处理")
    void testDetectAudioFormatMp3() throws Exception {
        assertEquals("mp3", detect(new byte[] {'I', 'D', '3', 0}));
        assertEquals("mp3", detect(new byte[] {(byte) 0xFF, (byte) 0xFB, 0, 0}));
    }

    @Test
    @DisplayName("detectAudioFormat - 数据不足 4 字节时按 mp3 兜底")
    void testDetectAudioFormatShortData() throws Exception {
        int[] sbox = NcmFormat.buildSbox("testkey".getBytes());
        Path file = tempDir.resolve("short.enc");
        Files.write(file, new byte[2]);

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            assertEquals("mp3", NcmFormat.detectAudioFormat(raf, sbox));
            assertEquals(0, raf.getFilePointer());
        }
    }

    /**
     * 构造以密钥流异或的音频头并嗅探，同时验证嗅探后文件指针复位
     */
    private String detect(byte[] plainHead) throws Exception {
        int[] sbox = NcmFormat.buildSbox("testkey".getBytes());
        byte[] keystream = NcmFormat.buildKeystream(sbox);

        byte[] data = new byte[16];
        System.arraycopy(plainHead, 0, data, 0, plainHead.length);
        for (int i = 0; i < data.length; i++) {
            data[i] ^= keystream[i];
        }
        Path file = tempDir.resolve("head.enc");
        Files.write(file, data);

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            String format = NcmFormat.detectAudioFormat(raf, sbox);
            assertEquals(0, raf.getFilePointer());
            return format;
        }
    }
}
