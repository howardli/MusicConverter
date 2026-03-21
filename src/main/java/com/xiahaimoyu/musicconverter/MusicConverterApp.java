package com.xiahaimoyu.musicconverter;

import com.xiahaimoyu.musicconverter.convertor.ConverterRegistry;
import com.xiahaimoyu.musicconverter.convertor.CopyConverter;
import com.xiahaimoyu.musicconverter.convertor.netease.NcmConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.xiahaimoyu.musicconverter.util.PathUtils.extractExtension;

/**
 * 音乐格式转换器
 * <p>
 * 支持解密多种加密音乐格式，保留元数据和目录结构
 */
public final class MusicConverterApp {

    private static final int EXIT_USAGE = 1;
    private static final int EXIT_SOURCE_NOT_FOUND = 2;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("用法: java -jar MusicConverter.jar <源目录> <目标目录>");
            System.exit(EXIT_USAGE);
        }

        Path source = Paths.get(args[0]).toAbsolutePath();
        Path target = Paths.get(args[1]).toAbsolutePath();

        if (Files.notExists(source)) {
            System.err.println("源目录不存在: " + source);
            System.exit(EXIT_SOURCE_NOT_FOUND);
        }

        ConverterRegistry registry = ConverterRegistry.of(
                new CopyConverter(),
                new NcmConverter()
                // new QmcConverter(),  // QQ音乐
                // new KgmConverter()   // 酷狗音乐
        );

        System.out.println("源目录: " + source);
        System.out.println("目标目录: " + target);
        System.out.println("支持格式: " + String.join(", ", registry.allExtensions()));
        System.out.println();

        processAllFiles(source, target, registry);
    }

    private static void processAllFiles(Path source, Path target, ConverterRegistry registry) throws Exception {
        AtomicInteger processed = new AtomicInteger();

        try (Stream<Path> files = Files.walk(source)) {
            files.parallel()
                 .filter(Files::isRegularFile)
                 .filter(f -> registry.supports(extractExtension(f.getFileName().toString())))
                 .forEach(file -> {
                     registry.find(extractExtension(file.getFileName().toString()))
                             .ifPresent(converter -> {
                                 converter.process(file, source, target);
                                 System.out.printf("\r已处理: %d 个文件    ", processed.incrementAndGet());
                             });
                 });
        }

        System.out.printf("%n完成! 共处理 %d 个文件%n", processed.get());
    }
}