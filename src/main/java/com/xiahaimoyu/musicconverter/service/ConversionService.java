package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.AudioDecoder;
import com.xiahaimoyu.musicconverter.plugin.FormatPlugin;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;
import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.util.PathUtils;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 转换服务
 * <p>
 * 编排完整的转换流程：查找插件、解码、写入元数据、音质比较、文件替换
 */
public final class ConversionService {

    private static final Logger LOG = Logger.getLogger(ConversionService.class.getName());

    static {
        // 关闭第三方库日志
        Logger.getLogger(ConverterConfig.LOG_PACKAGE_AUDIO_TAGGER).setLevel(Level.OFF);
    }

    private final PluginRegistry registry;
    private final QualityComparator qualityComparator;
    private final FileOrganizer fileOrganizer;

    public ConversionService(PluginRegistry registry) {
        this.registry = registry;
        this.qualityComparator = new QualityComparator();
        this.fileOrganizer = new FileOrganizer();
    }

    /**
     * 获取所有支持的格式
     */
    public Set<String> supportedFormats() {
        return registry.allExtensions();
    }

    /**
     * 处理单个文件
     *
     * @param source     源文件路径
     * @param sourceRoot 源目录根
     * @param targetRoot 目标目录根
     * @return 处理结果
     */
    public ConversionSummary.Result processFile(Path source, Path sourceRoot, Path targetRoot) {
        String extension = PathUtils.extractExtension(source.getFileName().toString());

        if (!registry.supports(extension)) {
            return ConversionSummary.Result.skipped();
        }

        try {
            return doProcessFile(source, sourceRoot, targetRoot, extension);
        } catch (ConverterException e) {
            LOG.log(Level.WARNING, "转换失败: " + source, e);
            return ConversionSummary.Result.failed(e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "转换失败: " + source, e);
            return ConversionSummary.Result.failed(
                    new ConverterException(ConverterException.ErrorCode.AUDIO_PROCESS_FAILED, source, e));
        }
    }

    private ConversionSummary.Result doProcessFile(Path source, Path sourceRoot,
                                                          Path targetRoot, String extension) throws Exception {
        // 1. 确保目标目录存在
        Path targetDir = fileOrganizer.ensureTargetDir(source, sourceRoot, targetRoot);

        // 2. 查找插件
        FormatPlugin plugin = registry.find(extension).orElseThrow(() ->
                new ConverterException(ConverterException.ErrorCode.UNSUPPORTED_FORMAT, source));

        // 3. 创建解码器并解码
        AudioDecoder decoder = plugin.createDecoder();
        Path tempBase = fileOrganizer.generateTempFile(targetDir, source);
        ConversionResult result = decoder.decode(source, tempBase);

        if (!result.isSuccess()) {
            return ConversionSummary.Result.skipped();
        }

        Path tempFile = result.outputFile();
        AudioMetadata metadata = result.metadata();

        // 4. 写入元数据
        if (metadata != null) {
            plugin.createMetadataHandler().ifPresent(handler -> {
                try {
                    handler.writeMetadata(tempFile, metadata);
                } catch (ConverterException e) {
                    LOG.log(Level.WARNING, "元数据写入失败: " + tempFile, e);
                }
            });
        }

        // 5. 音质比较决定是否替换
        Path finalFile = fileOrganizer.resolveFinalFile(targetDir, tempFile, source);
        if (qualityComparator.shouldReplace(tempFile, finalFile)) {
            fileOrganizer.replace(tempFile, finalFile);
            return ConversionSummary.Result.success();
        } else {
            fileOrganizer.cleanup(tempFile);
            return ConversionSummary.Result.skipped();
        }
    }

    /**
     * 处理整个目录
     *
     * @param sourceRoot 源目录根
     * @param targetRoot 目标目录根
     * @return 转换统计
     */
    public ConversionSummary processDirectory(Path sourceRoot, Path targetRoot) {
        ConversionSummary.Builder summary = ConversionSummary.builder();

        // 先收集所有文件，以便显示进度条
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<Path> allFiles = files.filter(Files::isRegularFile).collect(Collectors.toList());

            ProgressBar pb = new ProgressBarBuilder()
                    .setTaskName("处理文件")
                    .setInitialMax(allFiles.size())
                    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .build();

            for (Path file : allFiles) {
                summary.incrementTotal();

                ConversionSummary.Result result = processFile(file, sourceRoot, targetRoot);

                if (result.isSuccess()) {
                    summary.incrementSuccess();
                } else if (result.isSkipped()) {
                    summary.incrementSkipped();
                } else {
                    summary.incrementFailed();
                    if (result.failure() != null) {
                        summary.addFailure(result.failure());
                    }
                }

                pb.step();
            }

            pb.close();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "目录处理失败", e);
        }

        return summary.build();
    }
}