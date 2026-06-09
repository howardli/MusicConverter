package com.xiahaimoyu.musicconverter;

import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import com.xiahaimoyu.musicconverter.service.ConversionService;
import com.xiahaimoyu.musicconverter.service.ConversionSummary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 音乐格式转换器
 * <p>
 * 支持解密多种加密音乐格式，保留元数据和目录结构
 */
public final class MusicConverterApp {

    private static final String VERSION = "MusicConverter 1.0";
    private static final int EXIT_USAGE_ERROR = 1;
    private static final int EXIT_SOURCE_NOT_FOUND = 2;

    public static void main(String[] args) {
        // 处理特殊参数
        if (args.length == 0) {
            printUsage();
            System.exit(EXIT_USAGE_ERROR);
            return;
        }

        if (isHelpArg(args[0])) {
            printUsage();
            return;
        }

        if (isVersionArg(args[0])) {
            System.out.println(VERSION);
            return;
        }

        // 正常参数解析
        if (args.length < 2) {
            printUsage();
            System.exit(EXIT_USAGE_ERROR);
            return;
        }

        Path source = Paths.get(args[0]).toAbsolutePath();
        Path target = Paths.get(args[1]).toAbsolutePath();

        if (!Files.exists(source)) {
            System.err.println("源目录不存在: " + source);
            System.exit(EXIT_SOURCE_NOT_FOUND);
            return;
        }

        ConversionService service = createService();

        System.out.println("源目录: " + source);
        System.out.println("目标目录: " + target);
        System.out.println("支持格式: " + String.join(", ", service.supportedFormats()));
        System.out.println();

        ConversionSummary summary = service.processDirectory(source, target);

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

    private static void printUsage() {
        System.out.println(VERSION);
        System.out.println();
        System.out.println("用法: java -jar MusicConverter.jar <源目录> <目标目录>");
        System.out.println();
        System.out.println("参数:");
        System.out.println("  源目录    包含加密音乐文件的目录");
        System.out.println("  目标目录  解密后文件输出的目录");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --help     显示帮助信息");
        System.out.println("  --version  显示版本信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar MusicConverter.jar \"D:\\Music\\网易云音乐\" \"D:\\Music\\Unlocked\"");
        System.out.println("  java -jar MusicConverter.jar /Users/xxx/Music/网易云 /Users/xxx/Music/Unlocked");
    }

    private static boolean isHelpArg(String arg) {
        return "--help".equals(arg) || "-h".equals(arg);
    }

    private static boolean isVersionArg(String arg) {
        return "--version".equals(arg) || "-V".equals(arg);
    }

    private static ConversionService createService() {
        PluginRegistry registry = PluginRegistry.of(
                new CopyPlugin(),
                new NeteasePlugin()
                // new QmcPlugin(),   // QQ音乐（待实现）
                // new KgmPlugin()    // 酜狗音乐（待实现）
        );
        return new ConversionService(registry);
    }
}