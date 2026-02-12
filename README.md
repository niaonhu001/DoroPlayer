# DoroPlayer

**简体中文** | [English](README.en.md)

基于 Android 的视频脚本播放器，支持与 `.funscript` 脚本同步，并将轴控制指令通过多种连接方式发送至 OSR 等设备。

## 功能概览

- **视频库**：选择本地文件夹作为视频库，自动扫描并展示视频列表，支持自定义标签管理。
- **脚本解析**：自动匹配与视频同名的 `.funscript` 文件，目前还不支持多文件脚本。
- **热力图**：按轴展示脚本运动强度热力图；根据当前播放进度生成轴控制指令。
- **全屏播放**：支持横屏/竖屏全屏。
- **多种连接方式**（设备设置）：
  - **串口**：USB 串口，按 VID:PID 选择设备，可配置波特率（开发者模式）。
  - **蓝牙串口**：经典蓝牙 SPP（RFCOMM）。
  - **TCP / UDP**：填写 IP 与端口。
  - **The Handy**：BLE GATT，使用 [Handy 协议]发送线性指令。
- **连接测试**：在设备设置中提供「连接测试」按钮，用于验证当前连接是否正常。
- **输出范围**：各轴（L0–R2）可设置 0%–100% 输出范围，脚本位置会映射到该范围后发送。
- **开发者模式**：通过密码（`doroplayer`）开启后，可配置发送格式前缀/尾缀、波特率等，并查看轴控制相关信息。

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material3）
- **Media3 (ExoPlayer)** 视频播放
- **DocumentFile** 扫描视频库
- **Protocol Buffers**：The Handy 通讯
- **USB 串口**：[usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **蓝牙**：经典蓝牙 SPP（串口）、BLE GATT（The Handy）

## 环境要求

- Android Studio 或兼容 IDE
- JDK 11+
- Android：minSdk 24，targetSdk 35+

## 构建与运行

```bash
# 克隆仓库
git clone https://github.com/niaonhu001/DoroPlayer.git
cd DoroPlayer

# 构建 Debug 包
./gradlew assembleDebug
```

在 Android Studio 中打开项目，连接设备或启动模拟器后点击 Run 即可安装运行。首次使用需在应用内选择视频库目录并授予所需权限（存储、蓝牙等按连接方式而定）。

## 项目结构

```
app/src/main/
├── java/com/example/funplayer/
│   ├── MainActivity.kt          # 主界面、设置、脚本解析、各连接发送逻辑
│   └── ui/theme/                # Compose 主题
├── proto/
│   └── handyplug.proto         # The Handy BLE 协议定义
├── res/                         # 图标、布局、字符串等资源
└── AndroidManifest.xml
```

设备设置（连接开关、连接类型、IP/端口、设备选择、前缀/尾缀、输出范围等）均持久化在应用 SharedPreferences 中。

## 脚本格式说明

- 根节点 `actions` 数组 → 作为 **L0** 轴。
- `axes` 数组，每项含 `id` 与 `actions` → 多轴（如 L0、L1、L2、R0 等）。
- 轴指令格式：`轴名 + 目标位置(0–99) + "I" + 时间(ms)`，例如 `L050I900`。

`example/` 目录下可放置示例 `.funscript` 与对应视频（示例视频已加入 `.gitignore`，可按需自行添加）。

## 开源协议

本项目采用 [MIT License](LICENSE)。

## 贡献与反馈

欢迎提交 Issue 与 Pull Request。若需新增连接方式或修改协议，可在对应分支中扩展连接类型与发送逻辑。
