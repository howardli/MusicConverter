package com.xiahaimoyu.musicconverter;

import com.xiahaimoyu.musicconverter.plugin.builtin.CopyPlugin;
import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.plugin.netease.NeteasePlugin;
import com.xiahaimoyu.musicconverter.service.ConversionService;
import com.xiahaimoyu.musicconverter.service.ConversionSummary;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 音乐格式转换器
 * <p>
 * 支持解密多种加密音乐格式，保留元数据和目录结构
 */
@Command(
    name = "MusicConverter",
    description = "解密加密音乐格式，保留元数据和目录结构",
    version = "MusicConverter 1.0",
    mixinStandardHelpOptions = true,
    showDefaultValues = true
)
public final class MusicConverterApp implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(MusicConverterApp.class.getName());

    private static final int EXIT_SOURCE_NOT_FOUND = 2;
    private static final int EXIT_TARGET_NOT_DIR = 3;

    @Parameters(index = "0", description = "源目录路径")
    private Path sourceDir;

    @Parameters(index = "1", description = "目标目录路径")
    private Path targetDir;

    @Option(names = {"-f", "--format"}, split = ",",
            description = "只处理指定格式（逗号分隔），如: ncm,mp3")
    private List<String> formats;

    @Option(names = {"-v", "--verbose"},
            description = "显示详细日志")
    private boolean verbose;

    @Option(names = {"-q", "--quiet"},
            description = "静默模式，只显示错误")
    private boolean quiet;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MusicConverterApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // 配置日志级别
        configureLogging();

        // 验证源目录
        if (!Files.exists(sourceDir)) {
            System.err.println("源目录不存在: " + sourceDir);
            return EXIT_SOURCE_NOT_FOUND;
        }

        // 验证目标目录（如果存在必须是目录）
        if (Files.exists(targetDir) && !Files.isDirectory(targetDir)) {
            System.err.println("目标路径不是目录: " + targetDir);
            return EXIT_TARGET_NOT_DIR;
        }

        ConversionService service = createService();

        System.out.println("源目录: " + sourceDir.toAbsolutePath());
        System.out.println("目标目录: " + targetDir.toAbsolutePath());

        Set<String> supportedFormats = service.supportedFormats();
        if (formats != null && !formats.isEmpty()) {
            supportedFormats = filterFormats(supportedFormats, formats);
            System.out.println("处理格式: " + String.join(", ", supportedFormats));
        } else {
            System.out.println("支持格式: " + String.join(", ", supportedFormats));
        }
        System.out.println();

        ConversionSummary summary = service.processDirectory(sourceDir, targetDir);

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

        return summary.failedCount() > 0 ? 1 : 0;
    }

    private void configureLogging() {
        Level level = Level.INFO;
        if (verbose) {
            level = Level.FINE;
        } else if (quiet) {
            level = Level.WARNING;
        }
        LOG.setLevel(level);
        Logger.getLogger("com.xiahaimoyu.musicconverter").setLevel(level);
    }

    private ConversionService createService() {
        PluginRegistry registry = PluginRegistry.of(
                new CopyPlugin(),
                new NeteasePlugin()
                // new QmcPlugin(),   // QQ音乐（待实现）
                // new KgmPlugin()    // 酷狗音乐（待实现）
        );
        return new ConversionService(registry);
    }

    private Set<String> filterFormats(Set<String> allFormats, List<String> requested) {
        Set<String> result = new java.util.HashSet<>();
        for (String fmt : requested) {
            String lower = fmt.toLowerCase().replace(".", "");
            if (allFormats.contains(lower)) {
                result.add(lower);
            }
        }
        return result;
    }
}