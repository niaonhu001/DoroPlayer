package com.example.funplayer

import java.util.UUID

internal const val PREFS_NAME = "funplayer_settings"
internal const val KEY_VIDEO_LIBRARY_URI = "video_library_uri"
internal const val KEY_VIDEO_LIBRARY_DISPLAY_NAME = "video_library_display_name"
internal const val KEY_VIDEO_LIBRARY_SOURCE = "video_library_source"
internal const val KEY_NAS_HOST = "nas_host"
internal const val KEY_NAS_SHARE = "nas_share"
internal const val KEY_NAS_USER = "nas_user"
internal const val KEY_NAS_PASSWORD = "nas_password"
internal const val KEY_NAS_PORT = "nas_port"
internal const val KEY_NAS_SUBPATH = "nas_subpath"
internal const val KEY_VIDEO_TAGS = "video_tags"
internal const val KEY_DEVELOPER_MODE = "developer_mode"
internal const val CONNECTION_TEST_MESSAGE = "L050I900L150I900L250I900R050I900R150I900R250I900"
internal const val KEY_CONNECTION_TYPE = "device_connection_type"
internal const val KEY_CONNECTION_ENABLED = "device_connection_enabled"
internal const val KEY_DEVICE_IP = "device_ip"
internal const val KEY_DEVICE_PORT = "device_port"
internal const val KEY_SEND_FORMAT_PREFIX = "send_format_prefix"
internal const val KEY_SEND_FORMAT_SUFFIX = "send_format_suffix"
internal const val KEY_SERIAL_DEVICE = "serial_device_id"
internal const val KEY_BAUD_RATE = "serial_baud_rate"
internal const val KEY_AXIS_RANGES = "axis_ranges"
internal const val KEY_HANDY_KEY = "handy_connection_key"
internal const val KEY_HANDY_AXIS = "handy_axis"
internal const val KEY_HANDY_DEVICE_ADDRESS = "handy_device_address"
internal const val KEY_TCODEBLE_DEVICE_ADDRESS = "tcodeble_device_address"
internal const val KEY_BT_SERIAL_DEVICE_ADDRESS = "bt_serial_device_address"
internal const val KEY_APP_LANGUAGE = "app_language"
internal const val TAG_FILE_VERSION = 1
internal const val THUMBNAIL_WIDTH = 320
internal const val THUMBNAIL_HEIGHT = 180
// 对于元数据提取（时长、封面），只需要下载视频头部即可
internal const val SMB_METADATA_COPY_LIMIT = 3L * 1024 * 1024 // 3MB 足够获取元数据
// 保留更大的限制给其他用途（如果有的话）
internal const val SMB_TEMP_COPY_LIMIT = 20L * 1024 * 1024
internal const val MAX_VISIBLE_TAGS_IN_ROW = 6
internal const val TAG_KEY_ALL = "all"

internal val HANDY_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
internal val HANDY_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
internal val BT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// NAS video cache constants
internal const val NAS_VIDEO_CACHE_DIR_NAME = "nas_video_cache"
internal const val NAS_VIDEO_CACHE_MAX_BYTES = 2L * 1024 * 1024 * 1024 // 2GB
