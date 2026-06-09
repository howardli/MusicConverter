package com.xiahaimoyu.musicconverter.plugin.netease;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NcmFormat 单元测试
 * <p>
 * 测试包级私有的纯算法方法
 */
class NcmFormatTest {

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
    @DisplayName("hex - 十六进制字符串转字节数组")
    void testHex() {
        byte[] result = NcmFormat.hex("48656C6C6F");
        assertEquals(5, result.length);
        assertEquals('H', result[0]);
        assertEquals('e', result[1]);
        assertEquals('l', result[2]);
        assertEquals('l', result[3]);
        assertEquals('o', result[4]);
    }

    @Test
    @DisplayName("hex - 空字符串返回空数组")
    void testHexEmpty() {
        byte[] result = NcmFormat.hex("");
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("hex - CORE_KEY 正确解析")
    void testHexCoreKey() {
        byte[] key = NcmFormat.hex("687A4852416D736F356B496E62617857");
        assertEquals(16, key.length);
        assertEquals('h', key[0]);  // 0x68
        assertEquals('z', key[1]);  // 0x7A
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
}