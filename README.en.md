# DoroPlayer

[简体中文](README.md) | **English**

An Android video script player that syncs with `.funscript` files and sends axis control commands to OSR and similar devices over multiple connection types.

## Features

- **Video library**: Pick a local folder as the library; videos are scanned and listed with custom tag support.
- **Script parsing**: Automatically matches `.funscript` files with the same name as the video; multi-file scripts are not supported yet.
- **Heatmap**: Per-axis script intensity heatmap; axis commands are generated from current playback position.
- **Fullscreen**: Landscape and portrait fullscreen.
- **Connection options** (Device settings):
  - **Serial**: USB serial, select device by VID:PID, configurable baud rate (developer mode).
  - **Bluetooth serial**: Classic Bluetooth SPP (RFCOMM).
  - **TCP / UDP**: IP and port.
  - **The Handy**: BLE GATT using the [Handy protocol]for linear commands.
- **Connection test**: A “Test connection” button in device settings to verify the current connection.
- **Output range**: Each axis (L0–R2) can be limited to 0%–100%; script positions are mapped to this range before sending.
- **Developer mode**: Unlock with password `doroplayer` to configure send format prefix/suffix, baud rate, and view axis control details.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material3)
- **Media3 (ExoPlayer)** for playback
- **DocumentFile** for scanning the video library
- **Protocol Buffers** for The Handy protocol
- **USB serial**: [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **Bluetooth**: Classic SPP (serial) and BLE GATT (The Handy)

## Requirements

- Android Studio or compatible IDE
- JDK 11+
- Android: minSdk 24, targetSdk 35+

## Build and run

```bash
# Clone
git clone https://github.com/niaonhu001/DoroPlayer.git
cd DoroPlayer

# Build debug APK
./gradlew assembleDebug
```

Open the project in Android Studio, connect a device or start an emulator, then Run. On first launch, choose the video library folder in the app and grant the required permissions (storage, Bluetooth, etc., depending on the connection type).

## Project structure

```
app/src/main/
├── java/com/example/funplayer/
│   ├── MainActivity.kt          # UI, settings, script parsing, connection send logic
│   └── ui/theme/                 # Compose theme
├── proto/
│   └── handyplug.proto          # The Handy BLE protocol
├── res/                          # Icons, layouts, strings, etc.
└── AndroidManifest.xml
```

Device settings (connection on/off, type, IP/port, device selection, prefix/suffix, output range) are stored in SharedPreferences.

## Script format

- Root `actions` array → used as **L0** axis.
- `axes` array with `id` and `actions` per item → multiple axes (e.g. L0, L1, L2, R0).
- Axis command format: `axis name + target position (0–99) + "I" + time (ms)`, e.g. `L050I900`.

You can put sample `.funscript` and matching video files under `example/` (sample videos are in `.gitignore`; add them locally if needed).

## License

This project is under the [MIT License](LICENSE).

## Contributing

Issues and Pull Requests are welcome. To add a new connection type or change the protocol, extend the connection enum and send logic in the appropriate branch.
