package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NcmDecoder 单元测试
 * <p>
 * 测试边界情况：文件不存在、格式无效、无元数据等
 */
class NcmDecoderTest {

    // 与 NcmFormat 一致的协议常量（仅用于构造测试文件）
    private static final byte[] CORE_KEY = hex("687A4852416D736F356B496E62617857");
    private static final byte[] META_KEY = hex("2331346C6A6B5F215C5D2630553C2728");

    private final NcmDecoder decoder = new NcmDecoder();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("decode - 文件不存在抛出 FILE_NOT_FOUND")
    void testDecodeFileNotFound() {
        Path source = tempDir.resolve("nonexistent.ncm");
        Path output = tempDir.resolve("output");

        ConverterException ex = assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
        assertEquals(ErrorCode.FILE_NOT_FOUND, ex.code());
    }

    @Test
    @DisplayName("decode - 非 NCM 格式文件抛出 INVALID_FORMAT")
    void testDecodeInvalidFormat() throws Exception {
        // 创建一个不是 NCM 格式的文件
        Path source = tempDir.resolve("fake.ncm");
        Files.writeString(source, "this is not a ncm file");

        Path output = tempDir.resolve("output");

        ConverterException ex = assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
        assertEquals(ErrorCode.INVALID_FORMAT, ex.code());
    }

    @Test
    @DisplayName("decode - 空文件抛出异常")
    void testDecodeEmptyFile() throws Exception {
        Path source = tempDir.resolve("empty.ncm");
        Files.writeString(source, "");

        Path output = tempDir.resolve("output");

        assertThrows(ConverterException.class,
                () -> decoder.decode(source, output));
    }

    @Test
    @DisplayName("decode - 无元数据 NCM 嗅探 ID3 音频头，输出 .mp3 扩展名")
    void testDecodeMetadatalessSniffsMp3() throws Exception {
        Path source = writeNcm("no_meta.ncm", null, audioWithHead("ID3\u0003"));

        Path output = tempDir.resolve("output");
        ConversionResult result = decoder.decode(source, output);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.mp3"), result.outputFile());
        assertTrue(Files.exists(tempDir.resolve("output.mp3")));
    }

    @Test
    @DisplayName("decode - 无元数据 NCM 嗅探 fLaC 音频头，输出 .flac 扩展名")
    void testDecodeMetadatalessSniffsFlac() throws Exception {
        Path source = writeNcm("no_meta.ncm", null, audioWithHead("fLaC"));

        Path output = tempDir.resolve("output");
        ConversionResult result = decoder.decode(source, output);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.flac"), result.outputFile());
        assertTrue(Files.exists(tempDir.resolve("output.flac")));
    }

    @Test
    @DisplayName("decode - 元数据含 format 时以元数据为准")
    void testDecodeUsesMetadataFormat() throws Exception {
        String json = "{\"musicName\":\"Test Song\",\"album\":\"Test Album\","
                + "\"format\":\"flac\",\"artist\":[[0,\"Singer\"]]}";
        Path source = writeNcm("with_meta.ncm", json, audioWithHead("ID3\u0003"));

        Path output = tempDir.resolve("output");
        ConversionResult result = decoder.decode(source, output);

        assertTrue(result.isSuccess());
        assertEquals(tempDir.resolve("output.flac"), result.outputFile());
        assertEquals("Test Song", result.metadata().title());
    }

    // === 测试辅助：构造最小 NCM 文件 ===

    /**
     * 构造最小 NCM 文件：密钥块 + 元数据块（metaJson 为 null 时长度为 0）+ 空封面 + 密钥流异或的音频
     */
    private Path writeNcm(String name, String metaJson, byte[] plainAudio) throws Exception {
        Path file = tempDir.resolve(name);
        byte[] rc4Key = "testkey".getBytes(StandardCharsets.UTF_8);

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            // 文件头：magic "CTEN" + 间隔
            raf.writeInt(0x4354454E);
            raf.write(new byte[6]);

            // 密钥块：xor(AES(neteasecloudmusic + rc4Key, CORE_KEY), 0x64)
            byte[] keyPlain = concat("neteasecloudmusic".getBytes(StandardCharsets.UTF_8), rc4Key);
            byte[] keyBlock = aesEncrypt(CORE_KEY, keyPlain);
            NcmFormat.xor(keyBlock, 0x64);
            writeBlock(raf, keyBlock);

            // 元数据块：xor("163 key(Don't modify):" + base64(AES(music:json)), 0x63)
            if (metaJson == null) {
                writeBlock(raf, new byte[0]);
            } else {
                byte[] metaPlain = ("music:" + metaJson).getBytes(StandardCharsets.UTF_8);
                byte[] block = concat("163 key(Don't modify):".getBytes(StandardCharsets.UTF_8),
                        Base64.getMimeEncoder().encode(aesEncrypt(META_KEY, metaPlain)));
                NcmFormat.xor(block, 0x63);
                writeBlock(raf, block);
            }

            // 封面：CRC 间隔 + 长度 0（无封面）
            raf.write(new byte[9]);
            raf.writeInt(0);

            // 音频数据：明文按密钥流异或
            int[] sbox = NcmFormat.buildSbox(rc4Key);
            byte[] keystream = NcmFormat.buildKeystream(sbox);
            byte[] audio = plainAudio.clone();
            for (int i = 0; i < audio.length; i++) {
                audio[i] ^= keystream[i & 0xFF];
            }
            raf.write(audio);
        }
        return file;
    }

    private void writeBlock(RandomAccessFile raf, byte[] data) throws IOException {
        raf.writeInt(Integer.reverseBytes(data.length));
        raf.write(data);
    }

    /**
     * 构造以 4 字节音频头开头的明文音频
     */
    private byte[] audioWithHead(String magic) {
        byte[] head = magic.getBytes(StandardCharsets.US_ASCII);
        byte[] audio = new byte[64];
        System.arraycopy(head, 0, audio, 0, head.length);
        for (int i = head.length; i < audio.length; i++) {
            audio[i] = (byte) i;
        }
        return audio;
    }

    private byte[] aesEncrypt(byte[] key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(data);
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] hex(String s) {
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return data;
    }
}
