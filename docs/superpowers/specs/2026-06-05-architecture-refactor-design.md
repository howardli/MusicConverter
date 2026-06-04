# MusicConverter 架构重塑设计

日期：2026-06-05

## 目标

将 MusicConverter 项目重构为大厂级代码质量，具备良好的扩展性，方便后续添加新平台（QQ音乐、酷狗等）。

## 设计原则

1. **单一职责** - 每个类只做一件事
2. **接口隔离** - 小接口优于大接口
3. **依赖倒置** - 高层模块依赖抽象，不依赖具体实现
4. **开闭原则** - 新增格式只需新增 plugin 包下的类

## 架构概览

### 包结构

```
src/main/java/com/xiahaimoyu/musicconverter/
├── MusicConverterApp.java              # 应用入口
├── config/
│   └── ConverterConfig.java            # 配置常量
├── exception/
│   └── ConverterException.java         # 统一异常 + ErrorCode
├── model/
│   ├── AudioMetadata.java              # 音频元数据
│   └── ConversionResult.java           # 转换结果
├── plugin/
│   ├── FormatPlugin.java               # 格式插件主接口
│   ├── Decoder.java                    # 解码器接口
│   ├── MetadataHandler.java            # 元数据处理接口
│   ├── AbstractPlugin.java             # 插件基类
│   ├── PluginRegistry.java             # 插件注册表
│   └── builtin/
│       ├── CopyPlugin.java             # 直接复制插件
│       └── UnsupportedFormatPlugin.java # 不支持格式占位
│   └── netease/
│       ├── NeteasePlugin.java          # 网易云插件入口
│       ├── NcmDecoder.java             # NCM 解码器
│       ├── NcmMetadataHandler.java     # NCM 元数据处理
│       └── NcmFormat.java              # NCM 格式解析工具
├── service/
│   ├── ConversionService.java          # 转换服务
│   ├── QualityComparator.java          # 音质比较服务
│   ├── FileOrganizer.java              # 文件组织服务
│   └── ConversionSummary.java          # 处理统计
└── util/
    ├── PathUtils.java                  # 路径工具
    ├── AudioUtils.java                 # 音频工具
    └── ImageUtils.java                 # 图片工具
```

## 核心组件设计

### 1. 异常体系

**ConverterException.java**

```java
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

    public ErrorCode code() { return code; }
    public Path file() { return file; }

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
        ErrorCode(String message) { this.message = message; }
        public String message() { return message; }
    }
}
```

### 2. 核心接口

**FormatPlugin.java** - 格式插件主接口

```java
public interface FormatPlugin {
    Set<String> supportedExtensions();
    Decoder createDecoder();
    Optional<MetadataHandler> createMetadataHandler();
    String pluginName();
}
```

**Decoder.java** - 解码器接口

```java
public interface Decoder {
    ConversionResult decode(Path source, Path output) throws ConverterException;
}
```

**MetadataHandler.java** - 元数据处理接口

```java
public interface MetadataHandler {
    void writeMetadata(Path file, AudioMetadata metadata) throws ConverterException;
}
```

### 3. 插件基类

**AbstractPlugin.java**

```java
public abstract class AbstractPlugin implements FormatPlugin {

    @Override
    public Decoder createDecoder() {
        return createDecoderInstance();
    }

    @Override
    public Optional<MetadataHandler> createMetadataHandler() {
        MetadataHandler handler = createMetadataHandlerInstance();
        return Optional.ofNullable(handler);
    }

    protected abstract Decoder createDecoderInstance();
    protected MetadataHandler createMetadataHandlerInstance() { return null; }
}
```

### 4. 模型类

**AudioMetadata.java**

```java
public final class AudioMetadata {
    private final String title;
    private final String album;
    private final List<String> artists;
    private final byte[] coverArt;
    private final String coverMimeType;
    private final String format;
    private final String lyrics;

    public static Builder builder() { return new Builder(); }

    public String primaryArtist() {
        return artists.isEmpty() ? "" : artists.get(0);
    }

    public String artistDisplay() {
        return String.join(", ", artists);
    }

    public boolean hasCoverArt() {
        return coverArt != null && coverArt.length > 0;
    }

    // Builder 内部类
}
```

**ConversionResult.java**

```java
public final class ConversionResult {
    private final Path outputFile;
    private final AudioMetadata metadata;
    private final boolean success;

    public static ConversionResult success(Path file, AudioMetadata meta) {
        return new ConversionResult(file, meta, true);
    }

    public static ConversionResult failure() {
        return new ConversionResult(null, null, false);
    }
}
```

### 5. 配置类

**ConverterConfig.java**

```java
public final class ConverterConfig {
    public static final int BUFFER_SIZE = 0x8000;
    public static final String TEMP_FILE_PREFIX = "tmp_";
    public static final int MAX_COVER_WIDTH = 800;
    public static final int MAX_COVER_HEIGHT = 800;
    public static final String LOG_PACKAGE_AUDIO_TAGGER = "org.jaudiotagger";

    private ConverterConfig() {}
}
```

### 6. 服务层

**ConversionService.java** - 转换流程编排

```java
public final class ConversionService {
    private final PluginRegistry registry;
    private final QualityComparator qualityComparator;
    private final FileOrganizer fileOrganizer;

    public ConversionService(PluginRegistry registry) {
        this.registry = registry;
        this.qualityComparator = new QualityComparator();
        this.fileOrganizer = new FileOrganizer();
    }

    public void processFile(Path source, Path sourceRoot, Path targetRoot) {
        // 1. 查找插件
        // 2. 创建解码器
        // 3. 解码
        // 4. 获取元数据处理器
        // 5. 写入元数据
        // 6. 音质比较决定是否替换
    }

    public ConversionSummary processDirectory(Path source, Path target) {
        // 并行处理所有文件
    }

    public Set<String> supportedFormats() {
        return registry.allExtensions();
    }
}
```

**ConversionSummary.java**

```java
public final class ConversionSummary {
    private final int totalFiles;
    private final int successCount;
    private final int skippedCount;
    private final int failedCount;
    private final List<ConverterException> failures;
}
```

### 7. 插件注册表

**PluginRegistry.java**

```java
public final class PluginRegistry {
    private final Map<String, FormatPlugin> plugins = new ConcurrentHashMap<>();

    public PluginRegistry register(FormatPlugin plugin) {
        plugin.supportedExtensions()
              .forEach(ext -> plugins.put(ext.toLowerCase(), plugin));
        return this;
    }

    public Optional<FormatPlugin> find(String extension) {
        return Optional.ofNullable(plugins.get(extension.toLowerCase()));
    }

    public boolean supports(String extension) {
        return plugins.containsKey(extension.toLowerCase());
    }

    public Set<String> allExtensions() {
        return Collections.unmodifiableSet(plugins.keySet());
    }

    public static PluginRegistry of(FormatPlugin... plugins) {
        PluginRegistry registry = new PluginRegistry();
        Stream.of(plugins).forEach(registry::register);
        return registry;
    }
}
```

### 8. 辅助服务

**QualityComparator.java**

```java
public final class QualityComparator {
    public boolean shouldReplace(Path newFile, Path oldFile) {
        if (!Files.exists(oldFile)) return true;
        return hasHigherQuality(newFile, oldFile);
    }

    private boolean hasHigherQuality(Path newFile, Path oldFile) {
        // 比较比特率和采样率
    }
}
```

**FileOrganizer.java**

```java
public final class FileOrganizer {
    public Path ensureTargetDir(Path source, Path sourceRoot, Path targetRoot);
    public Path resolveFinalFile(Path targetDir, String baseName, String extension);
    public void cleanup(Path tempFile);
}
```

## 具体插件实现

### CopyPlugin

```java
public final class CopyPlugin extends AbstractPlugin {
    private static final Set<String> EXTENSIONS = Set.of("mp3", "flac", "wav", "aac", "ogg", "m4a");

    @Override
    public String pluginName() { return "Direct Copy"; }
    @Override
    public Set<String> supportedExtensions() { return EXTENSIONS; }
    @Override
    protected Decoder createDecoderInstance() { return new CopyDecoder(); }
}
```

### NeteasePlugin

```java
public final class NeteasePlugin extends AbstractPlugin {
    private static final Set<String> EXTENSIONS = Set.of("ncm");

    @Override
    public String pluginName() { return "Netease Cloud Music"; }
    @Override
    public Set<String> supportedExtensions() { return EXTENSIONS; }
    @Override
    protected Decoder createDecoderInstance() { return new NcmDecoder(); }
    @Override
    protected MetadataHandler createMetadataHandlerInstance() { return new NcmMetadataHandler(); }
}
```

## 扩展新平台示例

添加 QQ 音乐 QMC 格式：

```java
// plugin/qmusic/
public final class QmcPlugin extends AbstractPlugin {
    private static final Set<String> EXTENSIONS = Set.of("qmc0", "qmcflac", "qmc2");

    @Override
    public String pluginName() { return "QQ Music"; }
    @Override
    public Set<String> supportedExtensions() { return EXTENSIONS; }
    @Override
    protected Decoder createDecoderInstance() { return new QmcDecoder(); }
    @Override
    protected MetadataHandler createMetadataHandlerInstance() { return new QmcMetadataHandler(); }
}
```

在 MusicConverterApp 中注册：

```java
PluginRegistry registry = PluginRegistry.of(
    new CopyPlugin(),
    new NeteasePlugin(),
    new QmcPlugin()  // 新增一行即可
);
```

## 实现顺序

1. 创建新包结构和基础类（config、exception、model）
2. 定义核心接口（plugin 包）
3. 实现服务层和工具类迁移
4. 实现内置插件（CopyPlugin）
5. 实现网易云插件（迁移 NCM 解密逻辑）
6. 重构应用入口
7. 清理旧代码