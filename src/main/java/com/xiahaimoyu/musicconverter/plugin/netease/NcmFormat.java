package com.xiahaimoyu.musicconverter.plugin.netease;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
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

    // NCM 文件结构常量
    private static final int HEADER_SIZE = 10;
    private static final int CRC_GAP_SIZE = 9;
    private static final int MAGIC_NTET = 0x4354454E;
    private static final int MAGIC_NCNE = 0x4E455443;

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
        return magic == MAGIC_NTET || magic == MAGIC_NCNE;
    }

    /**
     * 读取密钥并构建 RC4 S-Box
     */
    static int[] readKeyAndBuildSbox(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encryptedKey = readBlock(raf);
            xor(encryptedKey, 0x64);

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
     */
    static AudioMetadata readMetadata(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encrypted = readBlock(raf);
            xor(encrypted, 0x63);

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
     */
    static void decryptAudio(RandomAccessFile raf, int[] sbox, Path output) throws ConverterException {
        try (java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(output.toFile()), ConverterConfig.BUFFER_SIZE)) {

            byte[] buf = new byte[ConverterConfig.BUFFER_SIZE];
            int len;

            while ((len = raf.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    int j = (i + 1) & 0xFF;
                    buf[i] ^= sbox[(sbox[j] + sbox[(sbox[j] + j) & 0xFF]) & 0xFF];
                }
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, output, e);
        }
    }

    // === 私有辅助方法 ===

    private static AudioMetadata parseMetadata(JSONObject json) {
        return AudioMetadata.builder()
                .album(json.getString("album"))
                .title(json.getString("musicName"))
                .artists(extractArtists(json.getJSONArray("artist")))
                .format(json.getString("format"))
                .build();
    }

    private static List<String> extractArtists(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
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

    private static int[] buildSbox(byte[] key) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            int tmp = s[i];
            s[i] = s[j];
            s[j] = tmp;
        }
        return s;
    }

    private static void xor(byte[] data, int key) {
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