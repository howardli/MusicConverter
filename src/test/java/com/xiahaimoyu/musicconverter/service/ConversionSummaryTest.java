package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConversionSummary 单元测试
 */
class ConversionSummaryTest {

    @Test
    @DisplayName("Result.success - 创建成功结果")
    void testResultSuccess() {
        ConversionSummary.Result result = ConversionSummary.Result.success();

        assertTrue(result.isSuccess());
        assertFalse(result.isSkipped());
        assertNull(result.failure());
    }

    @Test
    @DisplayName("Result.skipped - 创建跳过结果")
    void testResultSkipped() {
        ConversionSummary.Result result = ConversionSummary.Result.skipped();

        assertFalse(result.isSuccess());
        assertTrue(result.isSkipped());
        assertNull(result.failure());
    }

    @Test
    @DisplayName("Result.failed - 创建失败结果")
    void testResultFailed() {
        ConverterException ex = new ConverterException(ErrorCode.FILE_NOT_FOUND, null);
        ConversionSummary.Result result = ConversionSummary.Result.failed(ex);

        assertFalse(result.isSuccess());
        assertFalse(result.isSkipped());
        assertNotNull(result.failure());
        assertEquals(ErrorCode.FILE_NOT_FOUND, result.failure().code());
    }

    @Test
    @DisplayName("Builder - 构建统计结果")
    void testBuilder() {
        ConversionSummary summary = ConversionSummary.builder()
                .incrementTotal()
                .incrementTotal()
                .incrementSuccess()
                .incrementSkipped()
                .incrementFailed()
                .addFailure(new ConverterException(ErrorCode.DECRYPT_FAILED, null))
                .build();

        assertEquals(2, summary.totalFiles());
        assertEquals(1, summary.successCount());
        assertEquals(1, summary.skippedCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.failures().size());
    }

    @Test
    @DisplayName("Builder - 空统计结果")
    void testBuilderEmpty() {
        ConversionSummary summary = ConversionSummary.builder().build();

        assertEquals(0, summary.totalFiles());
        assertEquals(0, summary.successCount());
        assertEquals(0, summary.skippedCount());
        assertEquals(0, summary.failedCount());
        assertTrue(summary.failures().isEmpty());
    }

    @Test
    @DisplayName("failures - 返回不可修改列表")
    void testFailuresUnmodifiable() {
        ConverterException ex = new ConverterException(ErrorCode.FILE_NOT_FOUND, null);
        ConversionSummary summary = ConversionSummary.builder()
                .addFailure(ex)
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                summary.failures().add(new ConverterException(ErrorCode.DECRYPT_FAILED, null)));
    }

    @Test
    @DisplayName("Builder - 并发递增不丢失计数")
    void testBuilderConcurrentIncrements() throws Exception {
        int threads = 8;
        int loops = 500;
        ConversionSummary.Builder builder = ConversionSummary.builder();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < loops; i++) {
                        builder.incrementTotal();
                        builder.incrementSuccess();
                        builder.incrementFailed();
                        builder.addFailure(new ConverterException(ErrorCode.DECRYPT_FAILED, null));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        ConversionSummary summary = builder.build();
        assertEquals(threads * loops, summary.totalFiles());
        assertEquals(threads * loops, summary.successCount());
        assertEquals(threads * loops, summary.failedCount());
        assertEquals(threads * loops, summary.failures().size());
    }
}