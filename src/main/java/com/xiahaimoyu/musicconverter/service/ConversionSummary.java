package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.exception.ConverterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 转换统计结果
 * <p>
 * 记录处理的文件总数、成功、跳过、失败数量及失败详情
 */
public final class ConversionSummary {

    private final int totalFiles;
    private final int successCount;
    private final int skippedCount;
    private final int failedCount;
    private final List<ConverterException> failures;

    private ConversionSummary(int totalFiles, int successCount, int skippedCount,
                              int failedCount, List<ConverterException> failures) {
        this.totalFiles = totalFiles;
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.failedCount = failedCount;
        this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
    }

    public int totalFiles() {
        return totalFiles;
    }

    public int successCount() {
        return successCount;
    }

    public int skippedCount() {
        return skippedCount;
    }

    public int failedCount() {
        return failedCount;
    }

    public List<ConverterException> failures() {
        return failures;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 内部类（线程安全）
     */
    public static final class Builder {
        private final AtomicInteger totalFiles = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger skippedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final List<ConverterException> failures = Collections.synchronizedList(new ArrayList<>());

        public Builder incrementTotal() {
            this.totalFiles.incrementAndGet();
            return this;
        }

        public Builder incrementSuccess() {
            this.successCount.incrementAndGet();
            return this;
        }

        public Builder incrementSkipped() {
            this.skippedCount.incrementAndGet();
            return this;
        }

        public Builder incrementFailed() {
            this.failedCount.incrementAndGet();
            return this;
        }

        public Builder addFailure(ConverterException e) {
            this.failures.add(e);
            return this;
        }

        public ConversionSummary build() {
            return new ConversionSummary(
                    totalFiles.get(),
                    successCount.get(),
                    skippedCount.get(),
                    failedCount.get(),
                    new ArrayList<>(failures));  // 复制以避免后续修改
        }
    }

    /**
     * 单文件处理结果（内部使用）
     */
    public static final class Result {
        private final boolean success;
        private final boolean skipped;
        private final ConverterException failure;

        private Result(boolean success, boolean skipped, ConverterException failure) {
            this.success = success;
            this.skipped = skipped;
            this.failure = failure;
        }

        public static Result success() {
            return new Result(true, false, null);
        }

        public static Result skipped() {
            return new Result(false, true, null);
        }

        public static Result failed(ConverterException e) {
            return new Result(false, false, e);
        }

        public boolean isSuccess() { return success; }
        public boolean isSkipped() { return skipped; }
        public ConverterException failure() { return failure; }
    }
}