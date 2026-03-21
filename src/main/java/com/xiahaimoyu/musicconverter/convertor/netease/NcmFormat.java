package com.xiahaimoyu.musicconverter.convertor.netease;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * NCM 格式解析器
 * <p>
 * 负责解析 NCM 文件的加密结构
 */
final class NcmFormat {

    private NcmFormat() {}

    // AES 密钥
    private static final byte[] CORE_KEY = hex("687A4852416D736F356B496E62617857");
    private static final byte[] META_KEY = hex("2331346C6A6B5F215C5D2630553C2728");

    // 结构偏移
    private static final int HEADER_SIZE = 10;
    private static final int CRC_GAP_SIZE = 9;

    // 前缀长度
    private static final int KEY_PREFIX = 17;    // "neteasecloudmusic"
    private static final int META_PREFIX = 22;   // "163 key(Don't modify):"
    private static final int JSON_PREFIX = 6;    // "music:"

    /**
     * 验证 NCM 文件头
     */
    static boolean isValid(RandomAccessFile raf) throws Exception {
        int magic = raf.readInt();
        raf.skipBytes(HEADER_SIZE - 4);
        return magic == 0x4354454E || magic == 0x4E455443;
    }

    /**
     * 读取密钥并构建 RC4 S-Box
     */
    static int[] readKeyAndBuildSbox(RandomAccessFile raf) throws Exception {
        byte[] encryptedKey = readBlock(raf);
        xor(encryptedKey, 0x64);

        byte[] decrypted = aesDecrypt(CORE_KEY, encryptedKey);
        byte[] rc4Key = new byte[decrypted.length - KEY_PREFIX];
        System.arraycopy(decrypted, KEY_PREFIX, rc4Key, 0, rc4Key.length);

        return buildSbox(rc4Key);
    }

    /**
     * 读取并解密元数据 JSON
     */
    static JSONObject readMetadata(RandomAccessFile raf) throws Exception {
        byte[] encrypted = readBlock(raf);
        xor(encrypted, 0x63);

        byte[] decoded = Base64.getMimeDecoder().decode(
                new String(encrypted, META_PREFIX, encrypted.length - META_PREFIX));
        byte[] decrypted = aesDecrypt(META_KEY, decoded);

        String json = new String(decrypted, JSON_PREFIX, decrypted.length - JSON_PREFIX, StandardCharsets.UTF_8);
        return JSON.parseObject(json);
    }

    /**
     * 读取封面图片
     */
    static byte[] readCoverArt(RandomAccessFile raf) throws Exception {
        raf.skipBytes(CRC_GAP_SIZE);
        int len = Integer.reverseBytes(raf.readInt());
        if (len <= 0) return null;

        byte[] data = new byte[len];
        raf.readFully(data);
        return data;
    }

    private static byte[] readBlock(RandomAccessFile raf) throws Exception {
        int len = Integer.reverseBytes(raf.readInt());
        byte[] data = new byte[len];
        raf.readFully(data);
        return data;
    }

    private static int[] buildSbox(byte[] key) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) s[i] = i;

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            int tmp = s[i]; s[i] = s[j]; s[j] = tmp;
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