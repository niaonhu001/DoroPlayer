# DoroPlayer

**简体中文** | [English](README.en.md)

基于 Android 的视频脚本播放器，支持与 `.funscript` 脚本同步，并将轴控制指令通过多种连接方式发送至 OSR 等设备。

## 功能概览

- **视频库**：支持**本地文件夹**或 **NAS（SMB）** 作为视频库，自动扫描并展示视频列表；支持自定义标签管理（见下方 [.tag 文件说明](#tag-文件说明)）。
- **脚本解析**：自动匹配与视频同名的 `.funscript` 文件；支持**单文件**（一个文件内多轴或根 `actions`）与**多文件**（每轴一个文件，如 `视频名.funscript`、`视频名.surge.funscript`）。
- **热力图**：按轴展示脚本运动强度热力图；根据当前播放进度生成轴控制指令。
- **全屏播放**：支持横屏/竖屏全屏；支持 SMB 直连播放。
- **多种连接方式**（设备设置）：
  - **串口**：USB 串口，按 VID:PID 选择设备，可配置波特率（开发者模式）。
  - **蓝牙串口**：经典蓝牙 SPP（RFCOMM）。
  - **TCP / UDP**：填写 IP 与端口。
  - **The Handy**：使用 [Handy 协议] 发送线性指令（待完善）。
  - **JoyPlay**：JoyPlay通讯协议（可配置前缀/尾缀）。
- **设备控制**：在设置页可单独控制各轴滑块并发送指令，或选择脚本文件（单文件/多轴多文件）单独运行，无需播放视频。
- **连接测试**：在设备设置中提供「连接测试」按钮，用于验证当前连接是否正常。
- **输出范围**：各轴（L0–R2）可设置 0%–100% 输出范围，脚本位置会映射到该范围后发送。
- **开发者模式**：通过密码（`doroplayer`）开启后，可配置发送格式前缀/尾缀、波特率等，并查看 BLE/轴控制相关 Log。

## .tag 文件说明

每个视频可对应一个**同目录、同名**的 `.tag` 文件，用于保存该视频的标签；与视频同目录便于备份和迁移。

- **文件名**：`视频名.tag`。例如 `卡路里.mp4` 对应 `卡路里.tag`（扩展名前的部分与视频一致）。
- **内容格式**：JSON，UTF-8 编码。例如：
  ```json
  {"version":1,"tags":["舞蹈","音乐","HMV"]}
  ```
  - `version`：保留字段，当前为 `1`，用于日后格式扩展。
  - `tags`：字符串数组，表示该视频的标签列表，顺序可保留。
- **优先级**：若存在对应 `.tag` 文件，则**优先从 .tag 读取标签**；若无 .tag 或读取失败，则回退到应用内 SharedPreferences 中保存的标签。
- **写入**：在应用内为视频添加或修改标签时，会**优先写入 .tag 文件**（若文件不存在，会在同目录下自动创建 `视频名.tag`），并同步更新 SharedPreferences，以保证列表与详情展示一致。
- **NAS/SMB**：当视频库为 NAS 时，扫描会识别同目录下的 `.tag` 文件（如 `smb://.../视频名.tag`），并支持从该文件读取标签；写入 .tag 需应用具有对该共享路径的写权限。

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material3）
- **Media3 (ExoPlayer)** 视频播放（含 SMB 数据源）
- **DocumentFile** 扫描本地视频库
- **Protocol Buffers**：The Handy BLE 通讯
- **USB 串口**：[usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **蓝牙**：经典蓝牙 SPP（串口）、BLE GATT（The Handy / JoyPlay）
- **NAS**：jcifs-ng（SMB 扫描、目录浏览、.tag/.funscript 读取及 SMB 直连播放）

## 环境要求

- Android Studio 或兼容 IDE
- JDK 11+
- Android：minSdk 24，targetSdk 36（适配 **Android 12–16**）

## 构建与运行

```bash
# 克隆仓库
git clone https://github.com/niaonhu001/DoroPlayer.git
cd DoroPlayer

# 构建 Debug 包
./gradlew assembleDebug
```

在 Android Studio 中打开项目，连接设备或启动模拟器后点击 Run 即可安装运行。首次使用需在应用内选择视频库目录（或配置 NAS）并授予所需权限（存储、蓝牙等按连接方式而定）。

## 项目结构

```
app/src/main/
├── java/com/example/funplayer/
│   ├── MainActivity.kt          # 主界面、设置、脚本解析、各连接发送逻辑、.tag 读写
│   └── ui/theme/                # Compose 主题
├── proto/
│   └── handyplug.proto          # The Handy BLE 协议定义
├── res/                         # 图标、布局、字符串等资源
└── AndroidManifest.xml
```

设备设置（连接开关、连接类型、IP/端口、设备选择、前缀/尾缀、输出范围等）均持久化在应用 SharedPreferences 中。

## 脚本格式说明

- **单文件**：
  - 根节点 `actions` 数组 → 作为 **L0** 轴。
  - `axes` 数组，每项含 `id` 与 `actions` → 多轴（如 L0、L1、L2、R0 等）。
- **多文件**（多轴各一文件）：与视频同目录，按「视频名 + 轴后缀 + .funscript」命名。
  - L0：`视频名.funscript`
  - 其他轴：`视频名.后缀.funscript`，例如 `视频名.surge.funscript`（L1）、`视频名.sway.funscript`（L2）等；轴与后缀对应关系见应用内多轴脚本逻辑。
- **轴指令格式**：`轴名 + 目标位置(0–99) + "I" + 时间(ms)`，例如 `L050I900`；多轴直接拼接。

`example/` 目录下可放置示例 `.funscript`、`.tag` 与对应视频（示例视频已加入 `.gitignore`，可按需自行添加）。

## 开源协议

本项目采用 [MIT License](LICENSE)。

## 贡献与反馈

欢迎提交 Issue 与 Pull Request。若需新增连接方式或修改协议，可在对应分支中扩展连接类型与发送逻辑。
