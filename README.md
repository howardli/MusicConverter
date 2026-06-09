# 🎵 MusicConverter

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

网易云音乐 NCM 格式解密工具，将加密的音乐文件转换为标准的 MP3/FLAC 格式。

## ✨ 功能特性

- 🔓 **NCM 解密** - 支持网易云音乐 NCM 加密格式
- 📝 **元数据保留** - 完整保留专辑、标题、艺术家、封面等信息
- 🎯 **音质优先** - 自动比较音质，保留高比特率/采样率版本
- 📁 **目录结构** - 保持原有文件夹层级结构
- 📊 **进度显示** - 彩色进度条，实时显示处理进度
- 🔧 **插件架构** - 模块化设计，支持快速扩展新平台
- 🧪 **单元测试** - 完整的测试覆盖

## 📥 下载安装

### 方式一：下载预编译版本

从 [Releases](https://github.com/xiahaimoyu/MusicConverter/releases) 页面下载最新版 `MusicConverter.jar`

### 方式二：自行编译

```bash
# 克隆项目
git clone https://github.com/xiahaimoyu/MusicConverter.git
cd MusicConverter

# 编译打包（需要 Maven 和 JDK 8+）
mvn clean package

# 生成的 JAR 文件位于 target/MusicConverter.jar
```

## 🚀 使用方法

### 基本用法

```bash
java -jar MusicConverter.jar <源目录> <目标目录>
```

### 命令行参数

```
Usage: MusicConverter [-hqvV] [-f=<formats>[,<formats>...]]... <sourceDir> <targetDir>
解密加密音乐格式，保留元数据和目录结构

      <sourceDir>   源目录路径
      <targetDir>   目标目录路径
  -f, --format=<formats>[,<formats>...]
                    只处理指定格式（逗号分隔），如: ncm,mp3
  -h, --help        显示帮助信息
  -q, --quiet       静默模式，只显示错误
  -v, --verbose     显示详细日志
  -V, --version     显示版本信息
```

### 示例

```bash
# 基本使用
java -jar MusicConverter.jar "/Users/xxx/Music/网易云音乐" "/Users/xxx/Music/Unlocked"

# 只处理 NCM 文件
java -jar MusicConverter.jar -f ncm "/Users/xxx/Music/网易云音乐" "/Users/xxx/Music/Unlocked"

# 显示详细日志
java -jar MusicConverter.jar -v "/Users/xxx/Music/网易云音乐" "/Users/xxx/Music/Unlocked"

# 静默模式
java -jar MusicConverter.jar -q "/Users/xxx/Music/网易云音乐" "/Users/xxx/Music/Unlocked"
```

### 运行结果

```
源目录: /Users/xxx/Music/网易云音乐
目标目录: /Users/xxx/Music/Unlocked
支持格式: ncm, mp3, flac, wav, aac, ogg, m4a

处理文件 ████████████████████████████████████ 100% 128/128
完成! 处理 128 个文件，成功 128，跳过 0，失败 0
```

## 📋 支持格式

| 格式 | 扩展名 | 说明 |
|------|--------|------|
| NCM | `.ncm` | 网易云音乐加密格式，解密后输出 MP3/FLAC |
| MP3 | `.mp3` | 直接复制 |
| FLAC | `.flac` | 直接复制 |
| 其他 | `.wav`, `.aac`, `.ogg`, `.m4a` | 直接复制 |

## 🏗️ 项目结构

```
src/main/java/com/xiahaimoyu/musicconverter/
├── MusicConverterApp.java           # 应用入口（picocli 命令行）
├── config/
│   └── ConverterConfig.java         # 配置常量
├── exception/
│   └── ConverterException.java      # 异常定义
├── model/
│   ├── AudioMetadata.java           # 音频元数据模型
│   └── ConversionResult.java        # 转换结果模型
├── plugin/                          # 插件系统
│   ├── FormatPlugin.java            # 插件主接口
│   ├── AudioDecoder.java            # 解码器接口
│   ├── MetadataHandler.java         # 元数据处理器接口
│   ├── AbstractPlugin.java          # 插件抽象基类
│   ├── PluginRegistry.java          # 插件注册表
│   ├── builtin/                     # 内置插件
│   │   ├── CopyPlugin.java          # 普通音频复制插件
│   │   └── CopyDecoder.java         # 复制解码器
│   └── netease/                     # 网易云音乐插件
│       ├── NeteasePlugin.java       # 插件入口
│       ├── NcmDecoder.java          # NCM 解码器
│       ├── NcmFormat.java           # NCM 格式解析
│       └── NcmMetadataHandler.java  # 元数据处理
├── service/                         # 服务层
│   ├── ConversionService.java       # 转换编排服务
│   ├── ConversionSummary.java       # 转换统计
│   ├── FileOrganizer.java           # 文件组织服务
│   └── QualityComparator.java       # 音质比较服务
└── util/
    ├── PathUtils.java               # 路径处理工具
    └── ImageUtils.java              # 图片处理工具

src/test/java/                        # 单元测试
├── util/PathUtilsTest.java
└── service/FileOrganizerTest.java
```

## 🔌 扩展开发

本项目采用插件化架构，支持快速扩展新平台。

### 添加新平台插件

1. 创建插件类，继承 `AbstractPlugin`：

```java
public final class QmcPlugin extends AbstractPlugin {
    
    @Override
    public String pluginName() {
        return "QQ Music";
    }
    
    @Override
    public Set<String> supportedExtensions() {
        return Collections.singleton("qmc");
    }
    
    @Override
    protected AudioDecoder createDecoderInstance() {
        return new QmcDecoder();
    }
    
    @Override
    protected MetadataHandler createMetadataHandlerInstance() {
        return new QmcMetadataHandler();
    }
}
```

2. 实现解码器 `AudioDecoder` 和元数据处理器 `MetadataHandler`

3. 在 `MusicConverterApp.java` 中注册插件：

```java
PluginRegistry registry = PluginRegistry.of(
    new CopyPlugin(),
    new NeteasePlugin(),
    new QmcPlugin()  // 新增插件
);
```

### 核心接口

| 接口 | 职责 |
|------|------|
| `FormatPlugin` | 插件主接口，定义支持的格式和组件创建 |
| `AudioDecoder` | 音频解码，将加密文件转为标准格式 |
| `MetadataHandler` | 元数据处理，写入专辑/艺术家/封面等信息 |

## 📦 依赖项

| 依赖 | 版本 | 说明 |
|------|------|------|
| jaudiotagger | 3.0.1 | 音频元数据处理 |
| fastjson2 | 2.0.62 | JSON 解析（安全升级版） |
| picocli | 4.7.6 | 命令行参数解析 |
| progressbar | 0.10.1 | 进度条显示 |
| junit-jupiter | 5.10.2 | 单元测试（test scope） |

## 🛠️ 环境要求

- **JDK**: 8 或更高版本
- **Maven**: 3.6+（仅编译时需要）
- **OS**: Windows / macOS / Linux

## 🧪 运行测试

```bash
mvn test
```

## ⚠️ 免责声明

本项目仅供学习交流使用，请勿用于商业用途。使用本工具解密的音乐文件仅供个人欣赏，请勿传播或用于其他侵权行为。支持正版音乐！

## 📄 许可证

[MIT License](LICENSE)