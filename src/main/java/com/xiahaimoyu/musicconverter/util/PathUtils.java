package com.xiahaimoyu.musicconverter.util;

import java.io.File;
import java.text.Normalizer;

/**
 * 路径工具类
 */
public final class PathUtils {

    private static final String ILLEGAL_CHARS_PATTERN = "[\\\\/:*?\"<>|]";

    private PathUtils() {}

    /**
     * Unicode NFC 规范化
     */
    public static String normalize(String filename) {
        return Normalizer.normalize(filename, Normalizer.Form.NFC);
    }

    /**
     * 获取文件基本名（不含扩展名）
     */
    public static String baseNameOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    /**
     * 获取扩展名（含点号）
     */
    public static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    /**
     * 替换文件名中的非法字符
     */
    public static String sanitize(String filename) {
        return filename.replaceAll(ILLEGAL_CHARS_PATTERN, "_");
    }

    /**
     * 提取文件扩展名（小写，无点号）
     */
    public static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : null;
    }
}