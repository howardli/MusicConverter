package com.xiahaimoyu.musicconverter.plugin.netease;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * NCM 格式解析工具
 * <p>
 * 负责解析 NCM 文件的加密结构，提取密钥、元数据、封面
 */
final class NcmFormat {

    // AES 密钥（硬编码）
    private static final byte[] CORE_KEY = hex("687A4852416D736F356B496E62617857");
    private static final byte[] META_KEY = hex("2331346C6A6B5F215C5D2630553C2728");

    // XOR 解密常量
    private static final int XOR_KEY_BLOCK = 0x64;
    private static final int XOR_META_BLOCK = 0x63;

    // NCM 文件结构常量
    private static final int HEADER_SIZE = 10;
    private static final int CRC_GAP_SIZE = 9;
    private static final int MAGIC_CTEN = 0x4354454E;   // "CTEN"
    private static final int MAGIC_NETC = 0x4E455443;   // "NETC"

    // 前缀长度
    private static final int KEY_PREFIX_LENGTH = 17;    // "neteasecloudmusic"
    private static final int META_PREFIX_LENGTH = 22;   // "163 key(Don't modify):"
    private static final int JSON_PREFIX_LENGTH = 6;    // "music:"

    private NcmFormat() {}

    /**
     * 验证 NCM 文件头
     */
    static boolean isValidFormat(RandomAccessFile raf) throws IOException {
        int magic = raf.readInt();
        raf.skipBytes(HEADER_SIZE - 4);
        return magic == MAGIC_CTEN || magic == MAGIC_NETC;
    }

    /**
     * 读取密钥并构建 RC4 S-Box
     */
    static int[] readKeyAndBuildSbox(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encryptedKey = readBlock(raf);
            xor(encryptedKey, XOR_KEY_BLOCK);

            byte[] decrypted = aesDecrypt(CORE_KEY, encryptedKey);
            byte[] rc4Key = new byte[decrypted.length - KEY_PREFIX_LENGTH];
            System.arraycopy(decrypted, KEY_PREFIX_LENGTH, rc4Key, 0, rc4Key.length);

            return buildSbox(rc4Key);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.DECRYPT_FAILED, null, e);
        }
    }

    /**
     * 读取并解析元数据
     * <p>
     * 某些 NCM 文件不含元数据（块长度为 0），此时返回空元数据
     */
    static AudioMetadata readMetadata(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encrypted = readBlock(raf);
            if (encrypted.length == 0) {
                return AudioMetadata.builder().build();
            }

            xor(encrypted, XOR_META_BLOCK);

            byte[] decoded = Base64.getMimeDecoder().decode(
                    new String(encrypted, META_PREFIX_LENGTH, encrypted.length - META_PREFIX_LENGTH));
            byte[] decrypted = aesDecrypt(META_KEY, decoded);

            String json = new String(decrypted, JSON_PREFIX_LENGTH, decrypted.length - JSON_PREFIX_LENGTH, StandardCharsets.UTF_8);
            JSONObject jsonObj = JSON.parseObject(json);

            return parseMetadata(jsonObj);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.METADATA_PARSE_FAILED, null, e);
        }
    }

    /**
     * 读取封面图片
     */
    static byte[] readCoverArt(RandomAccessFile raf) throws ConverterException {
        try {
            raf.skipBytes(CRC_GAP_SIZE);
            int len = Integer.reverseBytes(raf.readInt());
            if (len <= 0) {
                return null;
            }

            byte[] data = new byte[len];
            raf.readFully(data);
            return data;
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.COVER_ART_FAILED, null, e);
        }
    }

    /**
     * 解密音频数据
     * <p>
     * 预计算 256 字节密钥流，再循环异或到音频数据上
     */
    static void decryptAudio(RandomAccessFile raf, int[] sbox, Path output) throws ConverterException {
        byte[] keystream = buildKeystream(sbox);

        try (BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(output.toFile()), ConverterConfig.BUFFER_SIZE)) {

            byte[] buf = new byte[ConverterConfig.BUFFER_SIZE];
            int len;
            int pos = 0;

            while ((len = raf.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    buf[i] ^= keystream[pos];
                    pos = (pos + 1) & 0xFF;
                }
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, output, e);
        }
    }

    // === 辅助方法 ===

    static AudioMetadata parseMetadata(JSONObject json) {
        return AudioMetadata.builder()
                .album(json.getString("album"))
                .title(json.getString("musicName"))
                .artists(extractArtists(json.getJSONArray("artist")))
                .format(json.getString("format"))
                .build();
    }

    static List<String> extractArtists(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> artists = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            artists.add(arr.getJSONArray(i).getString(1));
        }
        return artists;
    }

    private static byte[] readBlock(RandomAccessFile raf) throws IOException {
        int len = Integer.reverseBytes(raf.readInt());
        byte[] data = new byte[len];
        raf.readFully(data);
        return data;
    }

    static int[] buildSbox(byte[] key) {
        int[] sbox = new int[256];
        for (int i = 0; i < 256; i++) {
            sbox[i] = i;
        }

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + sbox[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            int tmp = sbox[i];
            sbox[i] = sbox[j];
            sbox[j] = tmp;
        }
        return sbox;
    }

    /**
     * 根据 S-Box 构建 256 字节密钥流
     */
    private static byte[] buildKeystream(int[] sbox) {
        byte[] keystream = new byte[256];
        for (int i = 0; i < 256; i++) {
            int j = (i + 1) & 0xFF;
            keystream[i] = (byte) sbox[(sbox[j] + sbox[(sbox[j] + j) & 0xFF]) & 0xFF];
        }
        return keystream;
    }

    static void xor(byte[] data, int key) {
        for (int i = 0; i < data.length; i++) {
            data[i] ^= key;
        }
    }

    private static byte[] aesDecrypt(byte[] key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(data);
    }

    private static byte[] hex(String s) {
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return data;
    }
}
