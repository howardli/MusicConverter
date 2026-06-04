package com.xiahaimoyu.musicconverter.config;

/**
 * 转换器全局配置
 * <p>
 * 定义文件处理、封面图片、日志等配置常量
 */
public final class ConverterConfig {

    /** 文件读写缓冲区大小（32KB） */
    public static final int BUFFER_SIZE = 0x8000;

    /** 临时文件前缀 */
    public static final String TEMP_FILE_PREFIX = "tmp_";

    /** 封面图片最大宽度 */
    public static final int MAX_COVER_WIDTH = 800;

    /** 封面图片最大高度 */
    public static final int MAX_COVER_HEIGHT = 800;

    /** 需要关闭日志的第三方包 */
    public static final String LOG_PACKAGE_AUDIO_TAGGER = "org.jaudiotagger";

    private ConverterConfig() {
        // 禁止实例化
    }
}