# 🎵 MusicConverter

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/xiahaimoyu/MusicConverter/actions/workflows/build.yml/badge.svg)](https://github.com/xiahaimoyu/MusicConverter/actions/workflows/build.yml)

网易云音乐 NCM 格式解密工具，将加密的音乐文件转换为标准的 MP3/FLAC 格式。

## ✨ 功能特性

- 🔓 **NCM 解密** - 支持网易云音乐 NCM 加密格式
- 📝 **元数据保留** - 完整保留专辑、标题、艺术家、封面等信息
- 🎯 **音质优先** - 自动比较音质，保留高比特率/采样率版本
- 📁 **目录结构** - 保持原有文件夹层级结构
- ⚡ **并行处理** - 多线程处理，性能优异
- 🔧 **易扩展** - 良好的接口设计，支持快速添加新平台

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

### 示例

```bash
# Windows
java -jar MusicConverter.jar "D:\Music\网易云音乐" "D:\Music\Unlocked"

# macOS
java -jar MusicConverter.jar "/Users/xxx/Music/网易云音乐" "/Users/xxx/Music/Unlocked"

# Linux
java -jar MusicConverter.jar "/home/xxx/Music/网易云音乐" "/home/xxx/Music/Unlocked"
```

### 运行结果

```
源目录: /Users/xxx/Music/网易云音乐
目标目录: /Users/xxx/Music/Unlocked
支持格式: ncm, mp3, flac, wav, aac, ogg, m4a

已处理: 128 个文件
完成! 共处理 128 个文件
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
├── MusicConverterApp.java          # 应用入口
├── model/
│   └── MusicMeta.java              # 音乐元数据模型
├── convertor/
│   ├── MusicConverter.java         # 转换器接口
│   ├── AbstractConverter.java      # 抽象基类（模板方法）
│   ├── ConversionContext.java      # 转换上下文
│   ├── ConverterRegistry.java      # 转换器注册表
│   ├── CopyConverter.java          # 普通音频复制器
│   └── netease/
│       ├── NcmConverter.java       # NCM 转换器
│       └── NcmFormat.java          # NCM 格式解析
└── util/
    ├── PathUtils.java              # 路径处理工具
    ├── AudioUtils.java             # 音频处理工具
    └── ImageUtils.java             # 图片处理工具
```

## 📦 依赖项

| 依赖 | 版本 | 说明 |
|------|------|------|
| jaudiotagger | 3.0.1 | 音频元数据处理 |
| fastjson | 1.2.83 | JSON 解析 |

## 🛠️ 环境要求

- **JDK**: 8 或更高版本
- **Maven**: 3.6+（仅编译时需要）
- **OS**: Windows / macOS / Linux

## ⚠️ 免责声明

本项目仅供学习交流使用，请勿用于商业用途。使用本工具解密的音乐文件仅供个人欣赏，请勿传播或用于其他侵权行为。支持正版音乐！

## 📄 许可证

[MIT License](LICENSE)