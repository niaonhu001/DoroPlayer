# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

DoroPlayer 是一个基于 Android 的视频脚本播放器，与 `.funscript` 文件同步，并通过多种连接方式向 OSR 等设备发送轴控制指令。应用使用 Kotlin 和 Jetpack Compose (Material3) 构建。

## 构建和开发命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 安装 Debug 版本到连接的设备
./gradlew installDebug

# 运行单元测试
./gradlew test

# 运行设备测试
./gradlew connectedAndroidTest
```

## 架构设计

### 核心结构

应用采用单 Activity 架构，使用 Jetpack Compose UI：

- **MainActivity.kt** (`app/src/main/java/com/example/funplayer/MainActivity.kt`)：包含所有主要逻辑：
  - UI 组合和导航
  - 视频库管理（本地/SMB）
  - 脚本解析（.funscript 文件）
  - 设备连接处理（USB、蓝牙、TCP/UDP、The Handy、TcodeBLE）
  - 标签管理（.tag 文件）
  - 通过 SharedPreferences 持久化设置

- **SmbDataSource.kt**：用于 SMB/NAS 视频流的自定义 Media3 数据源

- **Proto 文件**：`app/src/main/proto/handyplug.proto` - The Handy BLE 协议定义

### 脚本格式支持

应用支持两种脚本格式：
- **单文件**：根 `actions` 数组作为 L0 轴，或 `axes` 数组用于多轴
- **多文件**：每轴一个文件（如 `video.funscript`、`video.surge.funscript`）

### 设备连接类型

- USB 串口（使用 usb-serial-for-android）
- 蓝牙串口（SPP/RFCOMM）
- TCP/UDP 网络连接
- The Handy 协议（通过 Protocol Buffers）
- TcodeBLE 协议

### 标签系统

视频可以有对应的 `.tag` 文件（JSON 格式）存储在同一目录中：
```json
{"version":1,"tags":["舞蹈","音乐","HMV"]}
```

## 技术栈

- Kotlin + Jetpack Compose (Material3)
- Media3 (ExoPlayer) 用于视频播放
- DocumentFile 用于本地视频扫描
- Protocol Buffers 用于 The Handy BLE 通信
- jcifs-ng 用于 SMB/NAS 支持
- USB Serial for Android 库

## Android 兼容性

- minSdk: 24 (Android 7.0+)
- targetSdk: 36 (支持 Android 12–16)
- Java 11 兼容性

## 核心功能实现

- 支持本地文件夹或 NAS (SMB) 的视频库
- 脚本解析和与视频匹配
- 按轴显示脚本运动强度热力图
- 支持横屏/竖屏的全屏播放
- 直接 SMB 视频流
- 可配置每轴输出范围（0-100%）的设备控制
- 开发者模式（密码：`doroplayer`）用于高级设置

## 开发注意事项

- 所有设备设置都通过 SharedPreferences 持久化
- 应用使用 Media3 进行视频播放，包括 SMB 数据源
- Protocol Buffers 用于 The Handy BLE 通信
- USB 串口通信需要设备 VID:PID 配置
- 应用处理单轴和多轴脚本，自动文件检测