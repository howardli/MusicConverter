package com.xiahaimoyu.musicconverter;

import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import com.xiahaimoyu.musicconverter.service.ConversionService;
import com.xiahaimoyu.musicconverter.service.ConversionSummary;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 音乐格式转换器
 * <p>
 * 支持解密多种加密音乐格式，保留元数据和目录结构
 */
public final class MusicConverterApp {

    private static final int EXIT_USAGE_ERROR = 1;
    private static final int EXIT_SOURCE_NOT_FOUND = 2;

    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        if (cmd == null) {
            System.err.println("用法: java -jar MusicConverter.jar <源目录> <目标目录>");
            System.exit(EXIT_USAGE_ERROR);
            return;
        }

        if (!Files.exists(cmd.source())) {
            System.err.println("源目录不存在: " + cmd.source());
            System.exit(EXIT_SOURCE_NOT_FOUND);
            return;
        }

        ConversionService service = createService();

        System.out.println("源目录: " + cmd.source());
        System.out.println("目标目录: " + cmd.target());
        System.out.println("支持格式: " + String.join(", ", service.supportedFormats()));
        System.out.println();

        ConversionSummary summary = service.processDirectory(cmd.source(), cmd.target());

        System.out.println();
        System.out.printf("完成! 处理 %d 个文件，成功 %d，跳过 %d，失败 %d%n",
                summary.totalFiles(),
                summary.successCount(),
                summary.skippedCount(),
                summary.failedCount());

        if (!summary.failures().isEmpty()) {
            System.out.println("失败详情:");
            summary.failures().forEach(e ->
                    System.out.println("  - " + e.code().message() + ": " + e.file()));
        }
    }

    private static ConversionService createService() {
        PluginRegistry registry = PluginRegistry.of(
                new CopyPlugin(),
                new NeteasePlugin()
                // new QmcPlugin(),   // QQ音乐（待实现）
                // new KgmPlugin()    // 酷狗音乐（待实现）
        );
        return new ConversionService(registry);
    }

    private static CommandLine parseArgs(String[] args) {
        if (args.length < 2) {
            return null;
        }
        return new CommandLine(Path.of(args[0]).toAbsolutePath(), Path.of(args[1]).toAbsolutePath());
    }

    private record CommandLine(Path source, Path target) {}
}