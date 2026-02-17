# DoroPlayer

[简体中文](README.md) | **English**

An Android video script player that syncs with `.funscript` files and sends axis control commands to OSR and similar devices over multiple connection types.

## Features

- **Video library**: Use a **local folder** or **NAS (SMB)** as the library; videos are scanned and listed with custom tag support (see [.tag file format](#tag-file-format) below).
- **Script parsing**: Automatically matches `.funscript` files with the same base name as the video. Supports **single-file** scripts (multi-axis or root `actions` in one file) and **multi-file** scripts (one file per axis, e.g. `video.funscript`, `video.surge.funscript`).
- **Heatmap**: Per-axis script intensity heatmap; axis commands are generated from the current playback position.
- **Fullscreen**: Landscape and portrait fullscreen; supports direct SMB playback.
- **Connection options** (Device settings):
  - **Serial**: USB serial, select device by VID:PID, configurable baud rate (developer mode).
  - **Bluetooth serial**: Classic Bluetooth SPP (RFCOMM).
  - **TCP / UDP**: IP and port.
  - **The Handy**: BLE GATT using the [Handy protocol] for linear commands (to be refined).
  - **JoyPlay**: BLE GATT, text-format axis commands with configurable prefix/suffix .
- **Device control**: In settings you can control axis sliders and send commands, or pick a script file (single or multi-axis) to run without playing a video.
- **Connection test**: A “Test connection” button in device settings to verify the current connection.
- **Output range**: Each axis (L0–R2) can be limited to 0%–100%; script positions are mapped to this range before sending.
- **Developer mode**: Unlock with password `doroplayer` to configure send format prefix/suffix, baud rate, and view BLE/axis control logs.

## .tag file format

Each video can have a **same-name, same-directory** `.tag` file that stores that video’s tags, making backup and migration easy.

- **File name**: `videoname.tag`. For example, `video.mp4` uses `video.tag` (same base name as the video).
- **Content**: JSON, UTF-8. Example:
  ```json
  {"version":1,"tags":["dance","music","HMV"]}
  ```
  - `version`: Reserved; currently `1`, for future format changes.
  - `tags`: Array of strings, the tag list for that video; order is preserved.
- **Priority**: If a `.tag` file exists, tags are **read from it first**. If there is no .tag or read fails, the app falls back to tags stored in SharedPreferences.
- **Writing**: When you add or edit tags in the app, they are **written to the .tag file first** (the file is created in the same directory as `videoname.tag` if it does not exist), and SharedPreferences is updated so the list and detail views stay in sync.
- **NAS/SMB**: When the library is on NAS, scanning detects `.tag` files in the same directory (e.g. `smb://.../videoname.tag`) and reads tags from them. Writing .tag requires write access to the share.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material3)
- **Media3 (ExoPlayer)** for playback (including SMB data source)
- **DocumentFile** for scanning the local video library
- **Protocol Buffers** for The Handy BLE protocol
- **USB serial**: [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **Bluetooth**: Classic SPP (serial), BLE GATT (The Handy / JoyPlay)
- **NAS**: jcifs-ng (SMB scan, folder browse, .tag/.funscript read, and direct SMB playback)

## Requirements

- Android Studio or compatible IDE
- JDK 11+
- Android: minSdk 24, targetSdk 36 (supports **Android 12–16**)

## Build and run

```bash
# Clone
git clone https://github.com/niaonhu001/DoroPlayer.git
cd DoroPlayer

# Build debug APK
./gradlew assembleDebug
```

Open the project in Android Studio, connect a device or start an emulator, then Run. On first launch, choose the video library folder (or configure NAS) in the app and grant the required permissions (storage, Bluetooth, etc., depending on the connection type).

## Project structure

```
app/src/main/
├── java/com/example/funplayer/
│   ├── MainActivity.kt          # Main UI, settings, script parsing, connection send logic, .tag read/write
│   └── ui/theme/                # Compose theme
├── proto/
│   └── handyplug.proto          # The Handy BLE protocol
├── res/                         # Icons, layouts, strings, etc.
└── AndroidManifest.xml
```

Device settings (connection on/off, type, IP/port, device selection, prefix/suffix, output range) are stored in SharedPreferences.

## Script format

- **Single file**:
  - Root `actions` array → **L0** axis.
  - `axes` array with `id` and `actions` per item → multiple axes (e.g. L0, L1, L2, R0).
- **Multi-file** (one file per axis): Same directory as the video; name by “videoname + axis suffix + .funscript”.
  - L0: `videoname.funscript`
  - Other axes: `videoname.suffix.funscript`, e.g. `videoname.surge.funscript` (L1), `videoname.sway.funscript` (L2); see the app’s multi-axis script logic for suffix mapping.
- **Axis command format**: `axis + target position (0–99) + "I" + time (ms)`, e.g. `L050I900`; multiple axes are concatenated.

You can put sample `.funscript`, `.tag`, and matching videos under `example/` (sample videos are in `.gitignore`; add them locally if needed).

## License

This project is under the [MIT License](LICENSE).

## Contributing

Issues and Pull Requests are welcome. To add a new connection type or change the protocol, extend the connection enum and send logic in the appropriate branch.
