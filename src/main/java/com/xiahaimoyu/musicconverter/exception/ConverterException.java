package com.xiahaimoyu.musicconverter.exception;

import java.nio.file.Path;

/**
 * 转换器统一异常
 * <p>
 * 使用错误码区分不同错误类型，关联文件路径用于诊断
 */
public final class ConverterException extends RuntimeException {

    private final ErrorCode code;
    private final Path file;

    public ConverterException(ErrorCode code, Path file) {
        super(code.message() + ": " + file);
        this.code = code;
        this.file = file;
    }

    public ConverterException(ErrorCode code, Path file, Throwable cause) {
        super(code.message() + ": " + file, cause);
        this.code = code;
        this.file = file;
    }

    public ErrorCode code() {
        return code;
    }

    public Path file() {
        return file;
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        FILE_NOT_FOUND("文件不存在"),
        INVALID_FORMAT("文件格式无效"),
        DECRYPT_FAILED("解密失败"),
        METADATA_PARSE_FAILED("元数据解析失败"),
        METADATA_WRITE_FAILED("元数据写入失败"),
        FILE_WRITE_FAILED("文件写入失败"),
        AUDIO_PROCESS_FAILED("音频处理失败"),
        COVER_ART_FAILED("封面图片处理失败"),
        UNSUPPORTED_FORMAT("不支持的文件格式");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}