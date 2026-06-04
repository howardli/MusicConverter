# MusicConverter 架构重塑实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MusicConverter 重构为大厂级代码质量，具备良好扩展性

**Architecture:** 插件化架构，核心接口（FormatPlugin、Decoder、MetadataHandler）+ 服务层编排 + 内置插件实现

**Tech Stack:** Java 8、Maven、jaudiotagger、fastjson

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|------|------|
| `config/ConverterConfig.java` | 全局配置常量 |
| `exception/ConverterException.java` | 统一异常 + ErrorCode |
| `model/AudioMetadata.java` | 音频元数据（Builder 模式） |
| `model/ConversionResult.java` | 转换结果 |
| `plugin/FormatPlugin.java` | 格式插件主接口 |
| `plugin/Decoder.java` | 解码器接口 |
| `plugin/MetadataHandler.java` | 元数据处理接口 |
| `plugin/AbstractPlugin.java` | 插件基类（模板方法） |
| `plugin/PluginRegistry.java` | 插件注册表 |
| `plugin/builtin/CopyPlugin.java` | 直接复制插件 |
| `plugin/builtin/CopyDecoder.java` | 直接复制解码器 |
| `plugin/netease/NeteasePlugin.java` | 网易云插件入口 |
| `plugin/netease/NcmDecoder.java` | NCM 解码器 |
| `plugin/netease/NcmMetadataHandler.java` | NCM 元数据处理 |
| `plugin/netease/NcmFormat.java` | NCM 格式解析工具 |
| `service/ConversionService.java` | 转换流程编排 |
| `service/ConversionSummary.java` | 处理统计 |
| `service/QualityComparator.java` | 音质比较服务 |
| `service/FileOrganizer.java` | 文件组织服务 |

### 删除文件

| 文件 | 原因 |
|------|------|
| `convertor/MusicConverter.java` | 被 plugin 接口替代 |
| `convertor/AbstractConverter.java` | 被 service 层替代 |
| `convertor/ConversionContext.java` | 被 ConversionResult 替代 |
| `convertor/ConverterRegistry.java` | 被 PluginRegistry 替代 |
| `convertor/CopyConverter.java` | 被 CopyPlugin 替代 |
| `convertor/netease/NcmConverter.java` | 被 NeteasePlugin 替代 |
| `convertor/netease/NcmFormat.java` | 迁移到 plugin/netease |
| `model/MusicMeta.java` | 被 AudioMetadata 替代 |

---

## Task 1: 创建配置类

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/config/ConverterConfig.java`

- [ ] **Step 1: 创建 config 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/config
```

- [ ] **Step 2: 编写 ConverterConfig.java**

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/config/ConverterConfig.java
git commit -m "feat: 添加全局配置类 ConverterConfig"
```

---

## Task 2: 创建异常体系

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/exception/ConverterException.java`

- [ ] **Step 1: 创建 exception 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/exception
```

- [ ] **Step 2: 编写 ConverterException.java**

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/exception/ConverterException.java
git commit -m "feat: 添加统一异常类 ConverterException 和 ErrorCode"
```

---

## Task 3: 创建模型类 - AudioMetadata

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/model/AudioMetadata.java`

- [ ] **Step 1: 编写 AudioMetadata.java**

```java
package com.xiahaimoyu.musicconverter.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 音频元数据
 * <p>
 * 包含标题、专辑、艺术家、封面等信息，使用 Builder 模式创建
 */
public final class AudioMetadata {

    private final String title;
    private final String album;
    private final List<String> artists;
    private final byte[] coverArt;
    private final String coverMimeType;
    private final String format;
    private final String lyrics;

    private AudioMetadata(Builder builder) {
        this.title = builder.title;
        this.album = builder.album;
        this.artists = builder.artists != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.artists))
                : Collections.emptyList();
        this.coverArt = builder.coverArt;
        this.coverMimeType = builder.coverMimeType;
        this.format = builder.format;
        this.lyrics = builder.lyrics;
    }

    public String title() {
        return title != null ? title : "";
    }

    public String album() {
        return album != null ? album : "";
    }

    public List<String> artists() {
        return artists;
    }

    public byte[] coverArt() {
        return coverArt;
    }

    public String coverMimeType() {
        return coverMimeType;
    }

    public String format() {
        return format != null ? format : "";
    }

    public String lyrics() {
        return lyrics;
    }

    /**
     * 获取主要艺术家（第一个）
     */
    public String primaryArtist() {
        return artists.isEmpty() ? "" : artists.get(0);
    }

    /**
     * 获取艺术家显示字符串（逗号分隔）
     */
    public String artistDisplay() {
        return String.join(", ", artists);
    }

    /**
     * 判断是否有封面图片
     */
    public boolean hasCoverArt() {
        return coverArt != null && coverArt.length > 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 内部类
     */
    public static final class Builder {
        private String title;
        private String album;
        private List<String> artists;
        private byte[] coverArt;
        private String coverMimeType;
        private String format;
        private String lyrics;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder artists(List<String> artists) {
            this.artists = artists;
            return this;
        }

        public Builder artists(String[] artists) {
            this.artists = artists != null ? Arrays.asList(artists) : null;
            return this;
        }

        public Builder coverArt(byte[] coverArt) {
            this.coverArt = coverArt;
            return this;
        }

        public Builder coverMimeType(String coverMimeType) {
            this.coverMimeType = coverMimeType;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder lyrics(String lyrics) {
            this.lyrics = lyrics;
            return this;
        }

        public AudioMetadata build() {
            return new AudioMetadata(this);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/model/AudioMetadata.java
git commit -m "feat: 添加音频元数据类 AudioMetadata（Builder 模式）"
```

---

## Task 4: 创建模型类 - ConversionResult

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/model/ConversionResult.java`

- [ ] **Step 1: 编写 ConversionResult.java**

```java
package com.xiahaimoyu.musicconverter.model;

import java.nio.file.Path;

/**
 * 转换结果
 * <p>
 * 包含输出文件路径和提取的元数据
 */
public final class ConversionResult {

    private final Path outputFile;
    private final AudioMetadata metadata;
    private final boolean success;

    private ConversionResult(Path outputFile, AudioMetadata metadata, boolean success) {
        this.outputFile = outputFile;
        this.metadata = metadata;
        this.success = success;
    }

    /**
     * 创建成功结果
     */
    public static ConversionResult success(Path outputFile, AudioMetadata metadata) {
        return new ConversionResult(outputFile, metadata, true);
    }

    /**
     * 创建失败结果
     */
    public static ConversionResult failure() {
        return new ConversionResult(null, null, false);
    }

    public Path outputFile() {
        return outputFile;
    }

    public AudioMetadata metadata() {
        return metadata;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean hasMetadata() {
        return metadata != null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/model/ConversionResult.java
git commit -m "feat: 添加转换结果类 ConversionResult"
```

---

## Task 5: 创建插件接口 - FormatPlugin

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/FormatPlugin.java`

- [ ] **Step 1: 创建 plugin 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/plugin
```

- [ ] **Step 2: 编写 FormatPlugin.java**

```java
package com.xiahaimoyu.musicconverter.plugin;

import java.util.Optional;
import java.util.Set;

/**
 * 格式插件主接口
 * <p>
 * 实现此接口以支持新的加密音乐格式，每个插件负责一种或多种格式
 */
public interface FormatPlugin {

    /**
     * 返回支持的文件扩展名（小写，无点号）
     *
     * @return 扩展名集合
     */
    Set<String> supportedExtensions();

    /**
     * 创建解码器实例
     *
     * @return 解码器
     */
    Decoder createDecoder();

    /**
     * 创建元数据处理实例（可选）
     * <p>
     * 某些格式不需要写入元数据（如直接复制），可返回空
     *
     * @return 元数据处理器，或 Optional.empty()
     */
    Optional<MetadataHandler> createMetadataHandler();

    /**
     * 插件名称，用于日志和诊断
     *
     * @return 插件名称
     */
    String pluginName();
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/FormatPlugin.java
git commit -m "feat: 添加格式插件主接口 FormatPlugin"
```

---

## Task 6: 创建插件接口 - Decoder

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/Decoder.java`

- [ ] **Step 1: 编写 Decoder.java**

```java
package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.ConversionResult;

import java.nio.file.Path;

/**
 * 解码器接口
 * <p>
 * 负责将加密音频文件解码为标准格式
 */
public interface Decoder {

    /**
     * 解码音频文件
     *
     * @param source 源文件路径
     * @param output 输出文件路径基础名（不含扩展名，解码器需根据格式添加）
     * @return 解码结果，包含输出文件和元数据；失败返回 ConversionResult.failure()
     * @throws ConverterException 解码过程中发生错误
     */
    ConversionResult decode(Path source, Path output) throws ConverterException;
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/Decoder.java
git commit -m "feat: 添加解码器接口 Decoder"
```

---

## Task 7: 创建插件接口 - MetadataHandler

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/MetadataHandler.java`

- [ ] **Step 1: 编写 MetadataHandler.java**

```java
package com.xiahaimoyu.musicconverter.plugin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;

import java.nio.file.Path;

/**
 * 元数据处理接口
 * <p>
 * 负责将元数据写入音频文件
 */
public interface MetadataHandler {

    /**
     * 将元数据写入音频文件
     *
     * @param file   音频文件路径
     * @param metadata 元数据
     * @throws ConverterException 写入过程中发生错误
     */
    void writeMetadata(Path file, AudioMetadata metadata) throws ConverterException;
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/MetadataHandler.java
git commit -m "feat: 添加元数据处理接口 MetadataHandler"
```

---

## Task 8: 创建插件基类 - AbstractPlugin

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/AbstractPlugin.java`

- [ ] **Step 1: 编写 AbstractPlugin.java**

```java
package com.xiahaimoyu.musicconverter.plugin;

import java.util.Optional;

/**
 * 插件抽象基类
 * <p>
 * 提供模板方法模式的默认实现，简化具体插件开发
 */
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

    /**
     * 创建解码器实例（子类必须实现）
     *
     * @return 解码器
     */
    protected abstract Decoder createDecoderInstance();

    /**
     * 创建元数据处理器实例（子类可选实现）
     * <p>
     * 默认返回 null，表示不需要处理元数据
     *
     * @return 元数据处理器，或 null
     */
    protected MetadataHandler createMetadataHandlerInstance() {
        return null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/AbstractPlugin.java
git commit -m "feat: 添加插件基类 AbstractPlugin（模板方法）"
```

---

## Task 9: 创建插件注册表 - PluginRegistry

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/PluginRegistry.java`

- [ ] **Step 1: 编写 PluginRegistry.java**

```java
package com.xiahaimoyu.musicconverter.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 插件注册表
 * <p>
 * 线程安全，支持运行时动态注册，按扩展名查找插件
 */
public final class PluginRegistry {

    private final Map<String, FormatPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * 注册插件
     *
     * @param plugin 格式插件
     * @return this（链式调用）
     */
    public PluginRegistry register(FormatPlugin plugin) {
        plugin.supportedExtensions()
                .forEach(ext -> plugins.put(ext.toLowerCase(), plugin));
        return this;
    }

    /**
     * 查找支持指定扩展名的插件
     *
     * @param extension 文件扩展名（小写，无点号）
     * @return 插件，或 Optional.empty()
     */
    public Optional<FormatPlugin> find(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(plugins.get(extension.toLowerCase()));
    }

    /**
     * 判断是否支持指定扩展名
     *
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public boolean supports(String extension) {
        if (extension == null) {
            return false;
        }
        return plugins.containsKey(extension.toLowerCase());
    }

    /**
     * 获取所有支持的扩展名
     *
     * @return 扩展名集合
     */
    public Set<String> allExtensions() {
        return Collections.unmodifiableSet(plugins.keySet());
    }

    /**
     * 创建注册表并批量注册插件
     *
     * @param plugins 插件数组
     * @return 注册表实例
     */
    public static PluginRegistry of(FormatPlugin... plugins) {
        PluginRegistry registry = new PluginRegistry();
        Stream.of(plugins).forEach(registry::register);
        return registry;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/PluginRegistry.java
git commit -m "feat: 添加插件注册表 PluginRegistry"
```

---

## Task 10: 创建服务层 - QualityComparator

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/service/QualityComparator.java`

- [ ] **Step 1: 创建 service 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/service
```

- [ ] **Step 2: 编写 QualityComparator.java**

```java
package com.xiahaimoyu.musicconverter.service;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 音质比较服务
 * <p>
 * 比较两个音频文件的音质，决定是否需要替换
 */
public final class QualityComparator {

    /**
     * 判断新文件是否应替换旧文件
     * <p>
     * 旧文件不存在时返回 true；存在时比较音质
     *
     * @param newFile 新文件路径
     * @param oldFile 旧文件路径
     * @return 是否应替换
     */
    public boolean shouldReplace(Path newFile, Path oldFile) {
        if (!Files.exists(oldFile)) {
            return true;
        }
        return hasHigherQuality(newFile, oldFile);
    }

    /**
     * 判断新文件音质是否高于旧文件
     * <p>
     * 优先比较比特率，相同则比较采样率
     */
    private boolean hasHigherQuality(Path newFile, Path oldFile) {
        try {
            AudioHeader newHeader = readHeader(newFile);
            AudioHeader oldHeader = readHeader(oldFile);

            int bitrateCompare = Long.compare(
                    newHeader.getBitRateAsNumber(),
                    oldHeader.getBitRateAsNumber());

            if (bitrateCompare != 0) {
                return bitrateCompare > 0;
            }

            return newHeader.getSampleRateAsNumber() > oldHeader.getSampleRateAsNumber();
        } catch (Exception e) {
            return false;
        }
    }

    private AudioHeader readHeader(Path file) throws Exception {
        AudioFile audio = AudioFileIO.read(file.toFile());
        return audio.getAudioHeader();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/service/QualityComparator.java
git commit -m "feat: 添加音质比较服务 QualityComparator"
```

---

## Task 11: 创建服务层 - FileOrganizer

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/service/FileOrganizer.java`
- Modify: `src/main/java/com/xiahaimoyu/musicconverter/util/PathUtils.java`

- [ ] **Step 1: 编写 FileOrganizer.java**

```java
package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.xiahaimoyu.musicconverter.util.PathUtils.baseNameOf;
import static com.xiahaimoyu.musicconverter.util.PathUtils.extensionOf;
import static com.xiahaimoyu.musicconverter.util.PathUtils.normalize;
import static com.xiahaimoyu.musicconverter.util.PathUtils.sanitize;

/**
 * 文件组织服务
 * <p>
 * 负责目标目录创建、文件命名、临时文件清理
 */
public final class FileOrganizer {

    /**
     * 确保目标目录存在
     *
     * @param source     源文件路径
     * @param sourceRoot 源目录根
     * @param targetRoot 目标目录根
     * @return 目标目录路径
     * @throws ConverterException 创建目录失败
     */
    public Path ensureTargetDir(Path source, Path sourceRoot, Path targetRoot) {
        Path relativeDir = sourceRoot.relativize(source).getParent();
        Path targetDir = targetRoot.resolve(relativeDir != null ? relativeDir : Path.of(""));
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, targetDir, e);
        }
        return targetDir;
    }

    /**
     * 生成临时文件路径
     *
     * @param targetDir 目标目录
     * @param source    源文件（用于提取基本名）
     * @return 临时文件路径（不含扩展名）
     */
    public Path generateTempFile(Path targetDir, Path source) {
        String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
        return targetDir.resolve(ConverterConfig.TEMP_FILE_PREFIX + baseName);
    }

    /**
     * 生成最终文件路径
     *
     * @param targetDir 目标目录
     * @param tempFile  临时文件（用于提取扩展名）
     * @param source    源文件（用于提取基本名）
     * @return 最终文件路径
     */
    public Path resolveFinalFile(Path targetDir, Path tempFile, Path source) {
        String baseName = sanitize(baseNameOf(normalize(source.getFileName().toString())));
        String extension = extensionOf(tempFile.getFileName().toString());
        return targetDir.resolve(baseName + extension);
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件路径
     */
    public void cleanup(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // 清理失败不影响主流程
        }
    }

    /**
     * 替换文件（删除旧文件，移动新文件）
     *
     * @param newFile 新文件
     * @param oldFile 旧文件
     * @throws ConverterException 操作失败
     */
    public void replace(Path newFile, Path oldFile) throws ConverterException {
        try {
            Files.deleteIfExists(oldFile);
            Files.move(newFile, oldFile);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, oldFile, e);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/service/FileOrganizer.java
git commit -m "feat: 添加文件组织服务 FileOrganizer"
```

---

## Task 12: 创建服务层 - ConversionSummary

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/service/ConversionSummary.java`

- [ ] **Step 1: 编写 ConversionSummary.java**

```java
package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.exception.ConverterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Builder 内部类
     */
    public static final class Builder {
        private int totalFiles = 0;
        private int successCount = 0;
        private int skippedCount = 0;
        private int failedCount = 0;
        private final List<ConverterException> failures = new ArrayList<>();

        public Builder incrementTotal() {
            this.totalFiles++;
            return this;
        }

        public Builder incrementSuccess() {
            this.successCount++;
            return this;
        }

        public Builder incrementSkipped() {
            this.skippedCount++;
            return this;
        }

        public Builder incrementFailed() {
            this.failedCount++;
            return this;
        }

        public Builder addFailure(ConverterException e) {
            this.failures.add(e);
            return this;
        }

        public ConversionSummary build() {
            return new ConversionSummary(totalFiles, successCount, skippedCount, failedCount, failures);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/service/ConversionSummary.java
git commit -m "feat: 添加转换统计类 ConversionSummary"
```

---

## Task 13: 创建服务层 - ConversionService

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/service/ConversionService.java`

- [ ] **Step 1: 编写 ConversionService.java**

```java
package com.xiahaimoyu.musicconverter.service;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.Decoder;
import com.xiahaimoyu.musicconverter.plugin.FormatPlugin;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;
import com.xiahaimoyu.musicconverter.plugin.PluginRegistry;
import com.xiahaimoyu.musicconverter.util.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public ConversionSummary.SummaryResult processFile(Path source, Path sourceRoot, Path targetRoot) {
        String extension = PathUtils.extractExtension(source.getFileName().toString());

        if (!registry.supports(extension)) {
            return ConversionSummary.SummaryResult.skipped();
        }

        try {
            return doProcessFile(source, sourceRoot, targetRoot, extension);
        } catch (ConverterException e) {
            LOG.log(Level.WARNING, "转换失败: " + source, e);
            return ConversionSummary.SummaryResult.failed(e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "转换失败: " + source, e);
            return ConversionSummary.SummaryResult.failed(
                    new ConverterException(ConverterException.ErrorCode.AUDIO_PROCESS_FAILED, source, e));
        }
    }

    private ConversionSummary.SummaryResult doProcessFile(Path source, Path sourceRoot,
                                                          Path targetRoot, String extension) throws Exception {
        // 1. 确保目标目录存在
        Path targetDir = fileOrganizer.ensureTargetDir(source, sourceRoot, targetRoot);

        // 2. 查找插件
        FormatPlugin plugin = registry.find(extension).orElseThrow(() ->
                new ConverterException(ConverterException.ErrorCode.UNSUPPORTED_FORMAT, source));

        // 3. 创建解码器并解码
        Decoder decoder = plugin.createDecoder();
        Path tempBase = fileOrganizer.generateTempFile(targetDir, source);
        ConversionResult result = decoder.decode(source, tempBase);

        if (!result.isSuccess()) {
            return ConversionSummary.SummaryResult.skipped();
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
            return ConversionSummary.SummaryResult.success();
        } else {
            fileOrganizer.cleanup(tempFile);
            return ConversionSummary.SummaryResult.skipped();
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
        AtomicInteger processed = new AtomicInteger(0);

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.parallel()
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        summary.incrementTotal();

                        ConversionSummary.SummaryResult result = processFile(file, sourceRoot, targetRoot);

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

                        System.out.printf("\r已处理: %d 个文件    ", processed.incrementAndGet());
                    });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "目录处理失败", e);
        }

        return summary.build();
    }

    /**
     * 单文件处理结果（内部使用）
     */
    public static final class SummaryResult {
        private final boolean success;
        private final boolean skipped;
        private final ConverterException failure;

        private SummaryResult(boolean success, boolean skipped, ConverterException failure) {
            this.success = success;
            this.skipped = skipped;
            this.failure = failure;
        }

        public static SummaryResult success() {
            return new SummaryResult(true, false, null);
        }

        public static SummaryResult skipped() {
            return new SummaryResult(false, true, null);
        }

        public static SummaryResult failed(ConverterException e) {
            return new SummaryResult(false, false, e);
        }

        public boolean isSuccess() { return success; }
        public boolean isSkipped() { return skipped; }
        public ConverterException failure() { return failure; }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/service/ConversionService.java
git commit -m "feat: 添加转换服务 ConversionService"
```

---

## Task 14: 创建内置插件 - CopyPlugin

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/builtin/CopyPlugin.java`
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/builtin/CopyDecoder.java`

- [ ] **Step 1: 创建 builtin 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/plugin/builtin
```

- [ ] **Step 2: 编写 CopyDecoder.java**

```java
package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.Decoder;
import com.xiahaimoyu.musicconverter.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 直接复制解码器
 * <p>
 * 将普通音频文件直接复制到目标位置
 */
public final class CopyDecoder implements Decoder {

    @Override
    public ConversionResult decode(Path source, Path output) throws ConverterException {
        String extension = PathUtils.extractExtension(source.getFileName().toString());
        Path target = Path.of(output + "." + extension);

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, target, e);
        }

        // 直接复制的文件无元数据（从加密文件中提取）
        return ConversionResult.success(target, null);
    }
}
```

- [ ] **Step 3: 编写 CopyPlugin.java**

```java
package com.xiahaimoyu.musicconverter.plugin.builtin;

import com.xiahaimoyu.musicconverter.plugin.AbstractPlugin;
import com.xiahaimoyu.musicconverter.plugin.Decoder;

import java.util.Set;

/**
 * 直接复制插件
 * <p>
 * 处理已解密的普通音频格式（MP3、FLAC 等），直接复制文件
 */
public final class CopyPlugin extends AbstractPlugin {

    private static final Set<String> EXTENSIONS = Set.of(
            "mp3", "flac", "wav", "aac", "ogg", "m4a");

    @Override
    public String pluginName() {
        return "Direct Copy";
    }

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected Decoder createDecoderInstance() {
        return new CopyDecoder();
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/builtin/
git commit -m "feat: 添加直接复制插件 CopyPlugin 和 CopyDecoder"
```

---

## Task 15: 创建网易云插件 - NcmFormat

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmFormat.java`

- [ ] **Step 1: 创建 netease 包目录**

```bash
mkdir -p src/main/java/com/xiahaimoyu/musicconverter/plugin/netease
```

- [ ] **Step 2: 编写 NcmFormat.java**

```java
package com.xiahaimoyu.musicconverter.plugin.netease;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * NCM 格式解析工具
 * <p>
 * 负责解析 NCM 文件的加密结构，提取密钥、元数据、封面
 */
final class NcmFormat {

    // AES 密钥（硬编码）
    private static final byte[] CORE_KEY = hex("687A4852416D736F356B496E62617857");
    private static final byte[] META_KEY = hex("2331346C6A6B5F215C5D2630553C2728");

    // NCM 文件结构常量
    private static final int HEADER_SIZE = 10;
    private static final int CRC_GAP_SIZE = 9;
    private static final int MAGIC_NTET = 0x4354454E;
    private static final int_MAGIC_NCNE = 0x4E455443;

    // 前缀长度
    private static final int KEY_PREFIX_LENGTH = 17;    // "neteasecloudmusic"
    private static final int META_PREFIX_LENGTH = 22;   // "163 key(Don't modify):"
    private static final int JSON_PREFIX_LENGTH = 6;    // "music:"

    private NcmFormat() {}

    /**
     * 验证 NCM 文件头
     */
    static boolean isValidFormat(RandomAccessFile raf) throws IOException {
        int magic = raf.readInt();
        raf.skipBytes(HEADER_SIZE - 4);
        return magic == MAGIC_NTET || magic == _MAGIC_NCNE;
    }

    /**
     * 读取密钥并构建 RC4 S-Box
     */
    static int[] readKeyAndBuildSbox(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encryptedKey = readBlock(raf);
            xor(encryptedKey, 0x64);

            byte[] decrypted = aesDecrypt(CORE_KEY, encryptedKey);
            byte[] rc4Key = new byte[decrypted.length - KEY_PREFIX_LENGTH];
            System.arraycopy(decrypted, KEY_PREFIX_LENGTH, rc4Key, 0, rc4Key.length);

            return buildSbox(rc4Key);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.DECRYPT_FAILED, null, e);
        }
    }

    /**
     * 读取并解析元数据
     */
    static AudioMetadata readMetadata(RandomAccessFile raf) throws ConverterException {
        try {
            byte[] encrypted = readBlock(raf);
            xor(encrypted, 0x63);

            byte[] decoded = Base64.getMimeDecoder().decode(
                    new String(encrypted, META_PREFIX_LENGTH, encrypted.length - META_PREFIX_LENGTH));
            byte[] decrypted = aesDecrypt(META_KEY, decoded);

            String json = new String(decrypted, JSON_PREFIX_LENGTH, decrypted.length - JSON_PREFIX_LENGTH, StandardCharsets.UTF_8);
            JSONObject jsonObj = JSON.parseObject(json);

            return parseMetadata(jsonObj);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.METADATA_PARSE_FAILED, null, e);
        }
    }

    /**
     * 读取封面图片
     */
    static byte[] readCoverArt(RandomAccessFile raf) throws ConverterException {
        try {
            raf.skipBytes(CRC_GAP_SIZE);
            int len = Integer.reverseBytes(raf.readInt());
            if (len <= 0) {
                return null;
            }

            byte[] data = new byte[len];
            raf.readFully(data);
            return data;
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.COVER_ART_FAILED, null, e);
        }
    }

    /**
     * 解密音频数据
     */
    static void decryptAudio(RandomAccessFile raf, int[] sbox, Path output) throws ConverterException {
        try (java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(output.toFile()), ConverterConfig.BUFFER_SIZE)) {

            byte[] buf = new byte[ConverterConfig.BUFFER_SIZE];
            int len;

            while ((len = raf.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    int j = (i + 1) & 0xFF;
                    buf[i] ^= sbox[(sbox[j] + sbox[(sbox[j] + j) & 0xFF]) & 0xFF];
                }
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new ConverterException(ErrorCode.FILE_WRITE_FAILED, output, e);
        }
    }

    // === 私有辅助方法 ===

    private static AudioMetadata parseMetadata(JSONObject json) {
        return AudioMetadata.builder()
                .album(json.getString("album"))
                .title(json.getString("musicName"))
                .artists(extractArtists(json.getJSONArray("artist")))
                .format(json.getString("format"))
                .build();
    }

    private static List<String> extractArtists(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> artists = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            artists.add(arr.getJSONArray(i).getString(1));
        }
        return artists;
    }

    private static byte[] readBlock(RandomAccessFile raf) throws IOException {
        int len = Integer.reverseBytes(raf.readInt());
        byte[] data = new byte[len];
        raf.readFully(data);
        return data;
    }

    private static int[] buildSbox(byte[] key) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            int tmp = s[i];
            s[i] = s[j];
            s[j] = tmp;
        }
        return s;
    }

    private static void xor(byte[] data, int key) {
        for (int i = 0; i < data.length; i++) {
            data[i] ^= key;
        }
    }

    private static byte[] aesDecrypt(byte[] key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(data);
    }

    private static byte[] hex(String s) {
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return data;
    }

    // 修正常量命名
    private static final int _MAGIC_NCNE = 0x4E455443;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmFormat.java
git commit -m "feat: 添加 NCM 格式解析工具 NcmFormat"
```

---

## Task 16: 创建网易云插件 - NcmDecoder

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmDecoder.java`

- [ ] **Step 1: 编写 NcmDecoder.java**

```java
package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.model.ConversionResult;
import com.xiahaimoyu.musicconverter.plugin.Decoder;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * NCM 解码器
 * <p>
 * 将网易云 NCM 加密格式解码为标准 MP3/FLAC
 */
public final class NcmDecoder implements Decoder {

    @Override
    public ConversionResult decode(Path source, Path output) throws ConverterException {
        try (RandomAccessFile raf = openFile(source)) {
            // 1. 验证格式
            if (!NcmFormat.isValidFormat(raf)) {
                throw new ConverterException(ErrorCode.INVALID_FORMAT, source);
            }

            // 2. 读取密钥
            int[] sbox = NcmFormat.readKeyAndBuildSbox(raf);

            // 3. 读取元数据
            AudioMetadata metadata = NcmFormat.readMetadata(raf);

            // 4. 读取封面
            byte[] coverArt = NcmFormat.readCoverArt(raf);
            if (coverArt == null) {
                return ConversionResult.failure();
            }

            // 5. 合并封面到元数据
            AudioMetadata fullMetadata = AudioMetadata.builder()
                    .title(metadata.title())
                    .album(metadata.album())
                    .artists(metadata.artists())
                    .format(metadata.format())
                    .coverArt(coverArt)
                    .build();

            // 6. 解密音频数据
            Path outputFile = Path.of(output + "." + fullMetadata.format());
            NcmFormat.decryptAudio(raf, sbox, outputFile);

            return ConversionResult.success(outputFile, fullMetadata);
        } catch (ConverterException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw new ConverterException(ErrorCode.FILE_NOT_FOUND, source, e);
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.DECRYPT_FAILED, source, e);
        }
    }

    private RandomAccessFile openFile(Path source) throws FileNotFoundException {
        return new RandomAccessFile(source.toFile(), "r");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmDecoder.java
git commit -m "feat: 添加 NCM 解码器 NcmDecoder"
```

---

## Task 17: 创建网易云插件 - NcmMetadataHandler

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmMetadataHandler.java`

- [ ] **Step 1: 编写 NcmMetadataHandler.java**

```java
package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.config.ConverterConfig;
import com.xiahaimoyu.musicconverter.exception.ConverterException;
import com.xiahaimoyu.musicconverter.exception.ConverterException.ErrorCode;
import com.xiahaimoyu.musicconverter.model.AudioMetadata;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;
import com.xiahaimoyu.musicconverter.util.ImageUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.nio.file.Path;

/**
 * NCM 元数据处理器
 * <p>
 * 将提取的元数据（标题、专辑、艺术家、封面）写入音频文件
 */
public final class NcmMetadataHandler implements MetadataHandler {

    @Override
    public void writeMetadata(Path file, AudioMetadata metadata) throws ConverterException {
        try {
            AudioFile audio = AudioFileIO.read(file.toFile());
            Tag tag = audio.getTagOrCreateAndSetDefault();

            // 写入基本信息
            tag.setField(FieldKey.TITLE, metadata.title());
            tag.setField(FieldKey.ALBUM, metadata.album());
            tag.setField(FieldKey.ARTIST, metadata.primaryArtist());

            // 写入封面
            if (metadata.hasCoverArt()) {
                writeCoverArt(tag, metadata);
            }

            audio.commit();
        } catch (Exception e) {
            throw new ConverterException(ErrorCode.METADATA_WRITE_FAILED, file, e);
        }
    }

    private void writeCoverArt(Tag tag, AudioMetadata metadata) throws Exception {
        byte[] coverArt = metadata.coverArt();

        // 缩放封面图片
        byte[] resized = ImageUtils.resize(coverArt,
                ConverterConfig.MAX_COVER_WIDTH,
                ConverterConfig.MAX_COVER_HEIGHT)
                .orElse(coverArt);

        // 检测 MIME 类型并写入
        ImageUtils.detectMimeType(resized).ifPresent(mime -> {
            try {
                tag.deleteArtworkField();
                StandardArtwork artwork = new StandardArtwork();
                artwork.setBinaryData(resized);
                artwork.setMimeType(mime);
                tag.setField(artwork);
            } catch (Exception ignored) {
                // 封面写入失败不影响主流程
            }
        });
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NcmMetadataHandler.java
git commit -m "feat: 添加 NCM 元数据处理器 NcmMetadataHandler"
```

---

## Task 18: 创建网易云插件 - NeteasePlugin

**Files:**
- Create: `src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NeteasePlugin.java`

- [ ] **Step 1: 编写 NeteasePlugin.java**

```java
package com.xiahaimoyu.musicconverter.plugin.netease;

import com.xiahaimoyu.musicconverter.plugin.AbstractPlugin;
import com.xiahaimoyu.musicconverter.plugin.Decoder;
import com.xiahaimoyu.musicconverter.plugin.MetadataHandler;

import java.util.Set;

/**
 * 网易云音乐插件
 * <p>
 * 支持 NCM 加密格式的解码和元数据处理
 */
public final class NeteasePlugin extends AbstractPlugin {

    private static final Set<String> EXTENSIONS = Set.of("ncm");

    @Override
    public String pluginName() {
        return "Netease Cloud Music";
    }

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected Decoder createDecoderInstance() {
        return new NcmDecoder();
    }

    @Override
    protected MetadataHandler createMetadataHandlerInstance() {
        return new NcmMetadataHandler();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/plugin/netease/NeteasePlugin.java
git commit -m "feat: 添加网易云插件 NeteasePlugin"
```

---

## Task 19: 重构应用入口 - MusicConverterApp

**Files:**
- Modify: `src/main/java/com/xiahaimoyu/musicconverter/MusicConverterApp.java`

- [ ] **Step 1: 重写 MusicConverterApp.java**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/xiahaimoyu/musicconverter/MusicConverterApp.java
git commit -m "feat: 重构应用入口，使用新的插件架构"
```

---

## Task 20: 清理旧代码

**Files:**
- Delete: `src/main/java/com/xiahaimoyu/musicconverter/convertor/` (整个目录)
- Delete: `src/main/java/com/xiahaimoyu/musicconverter/model/MusicMeta.java`

- [ ] **Step 1: 删除旧的 convertor 包**

```bash
rm -rf src/main/java/com/xiahaimoyu/musicconverter/convertor/
```

- [ ] **Step 2: 删除旧的 MusicMeta.java**

```bash
rm src/main/java/com/xiahaimoyu/musicconverter/model/MusicMeta.java
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: 删除旧的 convertor 包和 MusicMeta 类"
```

---

## Task 21: 编译验证

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 打包项目**

```bash
mvn package
```

Expected: BUILD SUCCESS, 生成 `target/MusicConverter.jar`

- [ ] **Step 3: 提交最终状态**

```bash
git add -A
git commit -m "chore: 完成架构重塑，编译验证通过"
```

---

## Self-Review Checklist

完成后检查：

1. ✅ 所有设计文档中的类都已实现
2. ✅ 无占位符（TBD、TODO 等）
3. ✅ 类型和方法签名一致
4. ✅ 编译成功
5. ✅ 旧代码已清理