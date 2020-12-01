package kaist.iclab.abclogger.collector

import android.app.Notification
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.bluetooth.*
import android.content.Intent
import android.hardware.SensorManager
import android.hardware.usb.UsbConstants
import android.media.AudioManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.view.KeyEvent
import com.google.android.gms.fitness.data.Device
import com.google.android.gms.location.DetectedActivity

private const val UNDEFINED = "UNDEFINED"

internal fun stringifyBluetoothDeviceBondState(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BluetoothDevice.BOND_BONDED -> "BONDED"
    BluetoothDevice.BOND_BONDING -> "BONDING"
    BluetoothDevice.BOND_NONE -> "NONE"
    else -> default
}

internal fun stringifyBluetoothDeviceType(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
    BluetoothDevice.DEVICE_TYPE_LE -> "LE"
    BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
    else -> default
}

internal fun stringifyBluetoothClass(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER -> "AUDIO_VIDEO_CAMCORDER"
    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "AUDIO_VIDEO_CAR_AUDIO"
    BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> "AUDIO_VIDEO_HANDSFREE"
    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "AUDIO_VIDEO_HEADPHONES"
    BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> "AUDIO_VIDEO_HIFI_AUDIO"
    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "AUDIO_VIDEO_LOUDSPEAKER"
    BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE -> "AUDIO_VIDEO_MICROPHONE"
    BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> "AUDIO_VIDEO_PORTABLE_AUDIO"
    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> "AUDIO_VIDEO_SET_TOP_BOX"
    BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> "AUDIO_VIDEO_UNCATEGORIZED"
    BluetoothClass.Device.AUDIO_VIDEO_VCR -> "AUDIO_VIDEO_VCR"
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> "AUDIO_VIDEO_VIDEO_CAMERA"
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING -> "AUDIO_VIDEO_VIDEO_CONFERENCING"
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER"
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> "AUDIO_VIDEO_VIDEO_GAMING_TOY"
    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR -> "AUDIO_VIDEO_VIDEO_MONITOR"
    BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> "AUDIO_VIDEO_WEARABLE_HEADSET"
    BluetoothClass.Device.COMPUTER_DESKTOP -> "COMPUTER_DESKTOP"
    BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> "COMPUTER_HANDHELD_PC_PDA"
    BluetoothClass.Device.COMPUTER_LAPTOP -> "COMPUTER_LAPTOP"
    BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> "COMPUTER_PALM_SIZE_PC_PDA"
    BluetoothClass.Device.COMPUTER_SERVER -> "COMPUTER_SERVER"
    BluetoothClass.Device.COMPUTER_UNCATEGORIZED -> "COMPUTER_UNCATEGORIZED"
    BluetoothClass.Device.COMPUTER_WEARABLE -> "COMPUTER_WEARABLE"
    BluetoothClass.Device.HEALTH_BLOOD_PRESSURE -> "HEALTH_BLOOD_PRESSURE"
    BluetoothClass.Device.HEALTH_DATA_DISPLAY -> "HEALTH_DATA_DISPLAY"
    BluetoothClass.Device.HEALTH_GLUCOSE -> "HEALTH_GLUCOSE"
    BluetoothClass.Device.HEALTH_PULSE_OXIMETER -> "HEALTH_PULSE_OXIMETER"
    BluetoothClass.Device.HEALTH_PULSE_RATE -> "HEALTH_PULSE_RATE"
    BluetoothClass.Device.HEALTH_THERMOMETER -> "HEALTH_THERMOMETER"
    BluetoothClass.Device.HEALTH_UNCATEGORIZED -> "HEALTH_UNCATEGORIZED"
    BluetoothClass.Device.HEALTH_WEIGHING -> "HEALTH_WEIGHING"
    BluetoothClass.Device.PHONE_CELLULAR -> "PHONE_CELLULAR"
    BluetoothClass.Device.PHONE_CORDLESS -> "PHONE_CORDLESS"
    BluetoothClass.Device.PHONE_ISDN -> "PHONE_ISDN"
    BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY -> "PHONE_MODEM_OR_GATEWAY"
    BluetoothClass.Device.PHONE_SMART -> "PHONE_SMART"
    BluetoothClass.Device.PHONE_UNCATEGORIZED -> "PHONE_UNCATEGORIZED"
    BluetoothClass.Device.TOY_CONTROLLER -> "TOY_CONTROLLER"
    BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE -> "TOY_DOLL_ACTION_FIGURE"
    BluetoothClass.Device.TOY_GAME -> "TOY_GAME"
    BluetoothClass.Device.TOY_ROBOT -> "TOY_ROBOT"
    BluetoothClass.Device.TOY_UNCATEGORIZED -> "TOY_UNCATEGORIZED"
    BluetoothClass.Device.TOY_VEHICLE -> "TOY_VEHICLE"
    BluetoothClass.Device.WEARABLE_GLASSES -> "WEARABLE_GLASSES"
    BluetoothClass.Device.WEARABLE_HELMET -> "WEARABLE_HELMET"
    BluetoothClass.Device.WEARABLE_JACKET -> "WEARABLE_JACKET"
    BluetoothClass.Device.WEARABLE_PAGER -> "WEARABLE_PAGER"
    BluetoothClass.Device.WEARABLE_UNCATEGORIZED -> "WEARABLE_UNCATEGORIZED"
    BluetoothClass.Device.WEARABLE_WRIST_WATCH -> "WEARABLE_WRIST_WATCH"
    else -> default
}

internal fun stringifyBluetoothProfileConnectionState(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
    BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
    BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
    BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
    else -> default
}

internal fun stringifyBluetoothA2dpPlayingState(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BluetoothA2dp.STATE_PLAYING -> "PLAYING"
    BluetoothA2dp.STATE_NOT_PLAYING -> "NOT_PLAYING"
    else -> default
}

internal fun stringifyActivityType(flag: Int?, default: String = UNDEFINED) = when (flag) {
    DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
    DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
    DetectedActivity.ON_FOOT -> "ON_FOOT"
    DetectedActivity.RUNNING -> "RUNNING"
    DetectedActivity.STILL -> "STILL"
    DetectedActivity.TILTING -> "TILTING"
    DetectedActivity.WALKING -> "WALKING"
    else -> default
}

internal fun stringifyAppUsageEvent(flag: Int?, bucket: Int? = null, default: String = UNDEFINED) = when (flag) {
    UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED"
    UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED"
    UsageEvents.Event.ACTIVITY_STOPPED -> "ACTIVITY_STOPPED"
    UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
    UsageEvents.Event.DEVICE_SHUTDOWN -> "DEVICE_SHUTDOWN"
    UsageEvents.Event.DEVICE_STARTUP -> "DEVICE_STARTUP"
    UsageEvents.Event.FOREGROUND_SERVICE_START -> "FOREGROUND_SERVICE_START"
    UsageEvents.Event.FOREGROUND_SERVICE_STOP -> "FOREGROUND_SERVICE_STOP"
    UsageEvents.Event.KEYGUARD_HIDDEN -> "KEYGUARD_HIDDEN"
    UsageEvents.Event.KEYGUARD_SHOWN -> "KEYGUARD_SHOWN"
    UsageEvents.Event.SCREEN_INTERACTIVE -> "SCREEN_INTERACTIVE"
    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "SCREEN_NON_INTERACTIVE"
    UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
    UsageEvents.Event.STANDBY_BUCKET_CHANGED -> when(bucket) {
        UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "STANDBY_BUCKET_CHANGED_ACTIVE"
        UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "STANDBY_BUCKET_CHANGED_FREQUENT"
        UsageStatsManager.STANDBY_BUCKET_RARE -> "STANDBY_BUCKET_CHANGED_RARE"
        UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "STANDBY_BUCKET_CHANGED_RESTRICTED"
        UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "STANDBY_BUCKET_CHANGED_WORKING_SET"
        else -> "STANDBY_BUCKET_CHANGED_NEVER"
    }
    UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
    /**
     * Event types below are hidden in API level; but, it actually is reported.
     */
    3 -> "END_OF_DAY"
    4 -> "CONTINUE_PREVIOUS_DAY"
    6 -> "SYSTEM_INTERACTION"
    9 -> "CHOOSER_ACTION"
    10 -> "NOTIFICATION_SEEN"
    12 -> "NOTIFICATION_INTERRUPTION"
    13 -> "SLICE_PINNED_PRIV"
    14 -> "SLICE_PINNED"
    21 -> "CONTINUING_FOREGROUND_SERVICE"
    22 -> "ROLLOVER_FOREGROUND_SERVICE"
    24 -> "ACTIVITY_DESTROYED"
    25 -> "FLUSH_TO_DISK"
    28 -> "USER_UNLOCKED"
    29 -> "USER_STOPPED"
    30 -> "LOCUS_ID_SET"
    else -> default
}

internal fun stringifyBatteryHealth(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
    BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
    BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
    else -> default
}

internal fun stringifyBatteryPlugType(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
    else -> default
}

internal fun stringifyBatteryStatus(flag: Int?, default: String = UNDEFINED) = when (flag) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
    BatteryManager.BATTERY_STATUS_FULL -> "FULL"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
    else -> default
}

internal fun stringifyCallType(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
    CallLog.Calls.MISSED_TYPE -> "MISSED"
    CallLog.Calls.VOICEMAIL_TYPE -> "VOICE_MAIL"
    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
    CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
    CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "ANSWERED_EXTERNALLY"
    else -> default
}

internal fun stringifyCallPresentation(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    CallLog.Calls.PRESENTATION_ALLOWED -> "ALLOWED"
    CallLog.Calls.PRESENTATION_PAYPHONE -> "PAYPHONE"
    CallLog.Calls.PRESENTATION_RESTRICTED -> "RESTRICTED"
    CallLog.Calls.PRESENTATION_UNKNOWN -> "UNKNOWN"
    else -> default
}


internal fun stringifyMessageType(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT -> "DRAFT"
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED -> "FAILED"
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX -> "INBOX"
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX -> "OUTBOX"
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED -> "QUEUED"
    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT -> "SENT"
    else -> default
}


internal fun stringifyBluetoothConnectionState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    BluetoothAdapter.STATE_CONNECTED -> "CONNECTED"
    BluetoothAdapter.STATE_CONNECTING -> "CONNECTING"
    BluetoothAdapter.STATE_DISCONNECTED -> "DISCONNECTED"
    BluetoothAdapter.STATE_DISCONNECTING -> "DISCONNECTING"
    else -> default
}

internal fun stringifyBluetoothState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    BluetoothAdapter.STATE_OFF -> "OFF"
    BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
    BluetoothAdapter.STATE_ON -> "ON"
    BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
    else -> default
}

internal fun stringifyBluetoothScanMode(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "CONNECTABLE"
    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "CONNECTABLE_DISCOVERABLE"
    BluetoothAdapter.SCAN_MODE_NONE -> "NONE"
    else -> default
}

internal fun stringifyBluetoothHeadsetAudioState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    BluetoothHeadset.STATE_AUDIO_CONNECTED -> "CONNECTED"
    BluetoothHeadset.STATE_AUDIO_CONNECTING -> "CONNECTING"
    BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> "DISCONNECTED"
    else -> default
}

internal fun stringifyUsbClass(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    UsbConstants.USB_CLASS_APP_SPEC -> "APP_SPEC"
    UsbConstants.USB_CLASS_AUDIO -> "AUDIO"
    UsbConstants.USB_CLASS_CDC_DATA -> "CDC_DATA"
    UsbConstants.USB_CLASS_COMM -> "COMM"
    UsbConstants.USB_CLASS_CONTENT_SEC -> "CONTENT_SEC"
    UsbConstants.USB_CLASS_CSCID -> "CSCID"
    UsbConstants.USB_CLASS_HID -> "HID"
    UsbConstants.USB_CLASS_HUB -> "HUB"
    UsbConstants.USB_CLASS_MASS_STORAGE -> "MASS_STORAGE"
    UsbConstants.USB_CLASS_MISC -> "MISC"
    UsbConstants.USB_CLASS_PER_INTERFACE -> "PER_INTERFACE"
    UsbConstants.USB_CLASS_PHYSICA -> "PHYSICA"
    UsbConstants.USB_CLASS_PRINTER -> "PRINTER"
    UsbConstants.USB_CLASS_STILL_IMAGE -> "STILL_IMAGE"
    UsbConstants.USB_CLASS_VENDOR_SPEC -> "VENDOR_SPEC"
    UsbConstants.USB_CLASS_VIDEO -> "VIDEO"
    UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "WIRELESS_CONTROLLER"
    else -> default
}

internal fun stringifyKeyMetaState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    KeyEvent.META_ALT_LEFT_ON -> "ALT_LEFT_ON"
    KeyEvent.META_ALT_MASK -> "ALT_MASK"
    KeyEvent.META_ALT_ON -> "ALT_ON"
    KeyEvent.META_ALT_RIGHT_ON -> "ALT_RIGHT_ON"
    KeyEvent.META_CAPS_LOCK_ON -> "CAPS_LOCK_ON"
    KeyEvent.META_CTRL_LEFT_ON -> "CTRL_LEFT_ON"
    KeyEvent.META_CTRL_MASK -> "CTRL_MASK"
    KeyEvent.META_CTRL_ON -> "CTRL_ON"
    KeyEvent.META_CTRL_RIGHT_ON -> "CTRL_RIGHT_ON"
    KeyEvent.META_FUNCTION_ON -> "FUNCTION_ON"
    KeyEvent.META_META_LEFT_ON -> "META_LEFT_ON"
    KeyEvent.META_META_MASK -> "META_MASK"
    KeyEvent.META_META_ON -> "META_ON"
    KeyEvent.META_META_RIGHT_ON -> "META_RIGHT_ON"
    KeyEvent.META_NUM_LOCK_ON -> "NUM_LOCK_ON"
    KeyEvent.META_SCROLL_LOCK_ON -> "SCROLL_LOCK_ON"
    KeyEvent.META_SHIFT_LEFT_ON -> "SHIFT_LEFT_ON"
    KeyEvent.META_SHIFT_MASK -> "SHIFT_MASK"
    KeyEvent.META_SHIFT_ON -> "SHIFT_ON"
    KeyEvent.META_SHIFT_RIGHT_ON -> "SHIFT_RIGHT_ON"
    KeyEvent.META_SYM_ON -> "SYM_ON"
    else -> default
}

internal fun stringifyKeyCode(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    KeyEvent.KEYCODE_0 -> "0"
    KeyEvent.KEYCODE_1 -> "1"
    KeyEvent.KEYCODE_11 -> "11"
    KeyEvent.KEYCODE_12 -> "12"
    KeyEvent.KEYCODE_2 -> "2"
    KeyEvent.KEYCODE_3 -> "3"
    KeyEvent.KEYCODE_3D_MODE -> "3D_MODE"
    KeyEvent.KEYCODE_4 -> "4"
    KeyEvent.KEYCODE_5 -> "5"
    KeyEvent.KEYCODE_6 -> "6"
    KeyEvent.KEYCODE_7 -> "7"
    KeyEvent.KEYCODE_8 -> "8"
    KeyEvent.KEYCODE_9 -> "9"
    KeyEvent.KEYCODE_A -> "A"
    KeyEvent.KEYCODE_ALL_APPS -> "ALL_APPS"
    KeyEvent.KEYCODE_ALT_LEFT -> "ALT_LEFT"
    KeyEvent.KEYCODE_ALT_RIGHT -> "ALT_RIGHT"
    KeyEvent.KEYCODE_APOSTROPHE -> "APOSTROPHE"
    KeyEvent.KEYCODE_APP_SWITCH -> "APP_SWITCH"
    KeyEvent.KEYCODE_ASSIST -> "ASSIST"
    KeyEvent.KEYCODE_AT -> "AT"
    KeyEvent.KEYCODE_AVR_INPUT -> "AVR_INPUT"
    KeyEvent.KEYCODE_AVR_POWER -> "AVR_POWER"
    KeyEvent.KEYCODE_B -> "B"
    KeyEvent.KEYCODE_BACK -> "BACK"
    KeyEvent.KEYCODE_BACKSLASH -> "BACKSLASH"
    KeyEvent.KEYCODE_BOOKMARK -> "BOOKMARK"
    KeyEvent.KEYCODE_BREAK -> "BREAK"
    KeyEvent.KEYCODE_BRIGHTNESS_DOWN -> "BRIGHTNESS_DOWN"
    KeyEvent.KEYCODE_BRIGHTNESS_UP -> "BRIGHTNESS_UP"
    KeyEvent.KEYCODE_BUTTON_1 -> "BUTTON_1"
    KeyEvent.KEYCODE_BUTTON_10 -> "BUTTON_10"
    KeyEvent.KEYCODE_BUTTON_11 -> "BUTTON_11"
    KeyEvent.KEYCODE_BUTTON_12 -> "BUTTON_12"
    KeyEvent.KEYCODE_BUTTON_13 -> "BUTTON_13"
    KeyEvent.KEYCODE_BUTTON_14 -> "BUTTON_14"
    KeyEvent.KEYCODE_BUTTON_15 -> "BUTTON_15"
    KeyEvent.KEYCODE_BUTTON_16 -> "BUTTON_16"
    KeyEvent.KEYCODE_BUTTON_2 -> "BUTTON_2"
    KeyEvent.KEYCODE_BUTTON_3 -> "BUTTON_3"
    KeyEvent.KEYCODE_BUTTON_4 -> "BUTTON_4"
    KeyEvent.KEYCODE_BUTTON_5 -> "BUTTON_5"
    KeyEvent.KEYCODE_BUTTON_6 -> "BUTTON_6"
    KeyEvent.KEYCODE_BUTTON_7 -> "BUTTON_7"
    KeyEvent.KEYCODE_BUTTON_8 -> "BUTTON_8"
    KeyEvent.KEYCODE_BUTTON_9 -> "BUTTON_9"
    KeyEvent.KEYCODE_BUTTON_A -> "BUTTON_A"
    KeyEvent.KEYCODE_BUTTON_B -> "BUTTON_B"
    KeyEvent.KEYCODE_BUTTON_C -> "BUTTON_C"
    KeyEvent.KEYCODE_BUTTON_L1 -> "BUTTON_L1"
    KeyEvent.KEYCODE_BUTTON_L2 -> "BUTTON_L2"
    KeyEvent.KEYCODE_BUTTON_MODE -> "BUTTON_MODE"
    KeyEvent.KEYCODE_BUTTON_R1 -> "BUTTON_R1"
    KeyEvent.KEYCODE_BUTTON_R2 -> "BUTTON_R2"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "BUTTON_SELECT"
    KeyEvent.KEYCODE_BUTTON_START -> "BUTTON_START"
    KeyEvent.KEYCODE_BUTTON_THUMBL -> "BUTTON_THUMBL"
    KeyEvent.KEYCODE_BUTTON_THUMBR -> "BUTTON_THUMBR"
    KeyEvent.KEYCODE_BUTTON_X -> "BUTTON_X"
    KeyEvent.KEYCODE_BUTTON_Y -> "BUTTON_Y"
    KeyEvent.KEYCODE_BUTTON_Z -> "BUTTON_Z"
    KeyEvent.KEYCODE_C -> "C"
    KeyEvent.KEYCODE_CALCULATOR -> "CALCULATOR"
    KeyEvent.KEYCODE_CALENDAR -> "CALENDAR"
    KeyEvent.KEYCODE_CALL -> "CALL"
    KeyEvent.KEYCODE_CAMERA -> "CAMERA"
    KeyEvent.KEYCODE_CAPS_LOCK -> "CAPS_LOCK"
    KeyEvent.KEYCODE_CAPTIONS -> "CAPTIONS"
    KeyEvent.KEYCODE_CHANNEL_DOWN -> "CHANNEL_DOWN"
    KeyEvent.KEYCODE_CHANNEL_UP -> "CHANNEL_UP"
    KeyEvent.KEYCODE_CLEAR -> "CLEAR"
    KeyEvent.KEYCODE_COMMA -> "COMMA"
    KeyEvent.KEYCODE_CONTACTS -> "CONTACTS"
    KeyEvent.KEYCODE_COPY -> "COPY"
    KeyEvent.KEYCODE_CTRL_LEFT -> "CTRL_LEFT"
    KeyEvent.KEYCODE_CTRL_RIGHT -> "CTRL_RIGHT"
    KeyEvent.KEYCODE_CUT -> "CUT"
    KeyEvent.KEYCODE_D -> "D"
    KeyEvent.KEYCODE_DEL -> "DEL"
    KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD_CENTER"
    KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
    KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> "DPAD_DOWN_LEFT"
    KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> "DPAD_DOWN_RIGHT"
    KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
    KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
    KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
    KeyEvent.KEYCODE_DPAD_UP_LEFT -> "DPAD_UP_LEFT"
    KeyEvent.KEYCODE_DPAD_UP_RIGHT -> "DPAD_UP_RIGHT"
    KeyEvent.KEYCODE_DVR -> "DVR"
    KeyEvent.KEYCODE_E -> "E"
    KeyEvent.KEYCODE_EISU -> "EISU"
    KeyEvent.KEYCODE_ENDCALL -> "ENDCALL"
    KeyEvent.KEYCODE_ENTER -> "ENTER"
    KeyEvent.KEYCODE_ENVELOPE -> "ENVELOPE"
    KeyEvent.KEYCODE_EQUALS -> "EQUALS"
    KeyEvent.KEYCODE_ESCAPE -> "ESCAPE"
    KeyEvent.KEYCODE_EXPLORER -> "EXPLORER"
    KeyEvent.KEYCODE_F -> "F"
    KeyEvent.KEYCODE_F1 -> "F1"
    KeyEvent.KEYCODE_F10 -> "F10"
    KeyEvent.KEYCODE_F11 -> "F11"
    KeyEvent.KEYCODE_F12 -> "F12"
    KeyEvent.KEYCODE_F2 -> "F2"
    KeyEvent.KEYCODE_F3 -> "F3"
    KeyEvent.KEYCODE_F4 -> "F4"
    KeyEvent.KEYCODE_F5 -> "F5"
    KeyEvent.KEYCODE_F6 -> "F6"
    KeyEvent.KEYCODE_F7 -> "F7"
    KeyEvent.KEYCODE_F8 -> "F8"
    KeyEvent.KEYCODE_F9 -> "F9"
    KeyEvent.KEYCODE_FOCUS -> "FOCUS"
    KeyEvent.KEYCODE_FORWARD -> "FORWARD"
    KeyEvent.KEYCODE_FORWARD_DEL -> "FORWARD_DEL"
    KeyEvent.KEYCODE_FUNCTION -> "FUNCTION"
    KeyEvent.KEYCODE_G -> "G"
    KeyEvent.KEYCODE_GRAVE -> "GRAVE"
    KeyEvent.KEYCODE_GUIDE -> "GUIDE"
    KeyEvent.KEYCODE_H -> "H"
    KeyEvent.KEYCODE_HEADSETHOOK -> "HEADSETHOOK"
    KeyEvent.KEYCODE_HELP -> "HELP"
    KeyEvent.KEYCODE_HENKAN -> "HENKAN"
    KeyEvent.KEYCODE_HOME -> "HOME"
    KeyEvent.KEYCODE_I -> "I"
    KeyEvent.KEYCODE_INFO -> "INFO"
    KeyEvent.KEYCODE_INSERT -> "INSERT"
    KeyEvent.KEYCODE_J -> "J"
    KeyEvent.KEYCODE_K -> "K"
    KeyEvent.KEYCODE_KANA -> "KANA"
    KeyEvent.KEYCODE_KATAKANA_HIRAGANA -> "KATAKANA_HIRAGANA"
    KeyEvent.KEYCODE_L -> "L"
    KeyEvent.KEYCODE_LANGUAGE_SWITCH -> "LANGUAGE_SWITCH"
    KeyEvent.KEYCODE_LAST_CHANNEL -> "LAST_CHANNEL"
    KeyEvent.KEYCODE_LEFT_BRACKET -> "LEFT_BRACKET"
    KeyEvent.KEYCODE_M -> "M"
    KeyEvent.KEYCODE_MANNER_MODE -> "MANNER_MODE"
    KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> "MEDIA_AUDIO_TRACK"
    KeyEvent.KEYCODE_MEDIA_CLOSE -> "MEDIA_CLOSE"
    KeyEvent.KEYCODE_MEDIA_EJECT -> "MEDIA_EJECT"
    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "MEDIA_FAST_FORWARD"
    KeyEvent.KEYCODE_MEDIA_NEXT -> "MEDIA_NEXT"
    KeyEvent.KEYCODE_MEDIA_PAUSE -> "MEDIA_PAUSE"
    KeyEvent.KEYCODE_MEDIA_PLAY -> "MEDIA_PLAY"
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "MEDIA_PLAY_PAUSE"
    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "MEDIA_PREVIOUS"
    KeyEvent.KEYCODE_MEDIA_RECORD -> "MEDIA_RECORD"
    KeyEvent.KEYCODE_MEDIA_REWIND -> "MEDIA_REWIND"
    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> "MEDIA_SKIP_BACKWARD"
    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> "MEDIA_SKIP_FORWARD"
    KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> "MEDIA_STEP_BACKWARD"
    KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> "MEDIA_STEP_FORWARD"
    KeyEvent.KEYCODE_MEDIA_STOP -> "MEDIA_STOP"
    KeyEvent.KEYCODE_MEDIA_TOP_MENU -> "MEDIA_TOP_MENU"
    KeyEvent.KEYCODE_MENU -> "MENU"
    KeyEvent.KEYCODE_META_LEFT -> "LEFT"
    KeyEvent.KEYCODE_META_RIGHT -> "RIGHT"
    KeyEvent.KEYCODE_MINUS -> "MINUS"
    KeyEvent.KEYCODE_MOVE_END -> "MOVE_END"
    KeyEvent.KEYCODE_MOVE_HOME -> "MOVE_HOME"
    KeyEvent.KEYCODE_MUHENKAN -> "MUHENKAN"
    KeyEvent.KEYCODE_MUSIC -> "MUSIC"
    KeyEvent.KEYCODE_MUTE -> "MUTE"
    KeyEvent.KEYCODE_N -> "N"
    KeyEvent.KEYCODE_NAVIGATE_IN -> "NAVIGATE_IN"
    KeyEvent.KEYCODE_NAVIGATE_NEXT -> "NAVIGATE_NEXT"
    KeyEvent.KEYCODE_NAVIGATE_OUT -> "NAVIGATE_OUT"
    KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> "NAVIGATE_PREVIOUS"
    KeyEvent.KEYCODE_NOTIFICATION -> "NOTIFICATION"
    KeyEvent.KEYCODE_NUM -> "NUM"
    KeyEvent.KEYCODE_NUMPAD_0 -> "NUMPAD_0"
    KeyEvent.KEYCODE_NUMPAD_1 -> "NUMPAD_1"
    KeyEvent.KEYCODE_NUMPAD_2 -> "NUMPAD_2"
    KeyEvent.KEYCODE_NUMPAD_3 -> "NUMPAD_3"
    KeyEvent.KEYCODE_NUMPAD_4 -> "NUMPAD_4"
    KeyEvent.KEYCODE_NUMPAD_5 -> "NUMPAD_5"
    KeyEvent.KEYCODE_NUMPAD_6 -> "NUMPAD_6"
    KeyEvent.KEYCODE_NUMPAD_7 -> "NUMPAD_7"
    KeyEvent.KEYCODE_NUMPAD_8 -> "NUMPAD_8"
    KeyEvent.KEYCODE_NUMPAD_9 -> "NUMPAD_9"
    KeyEvent.KEYCODE_NUMPAD_ADD -> "NUMPAD_ADD"
    KeyEvent.KEYCODE_NUMPAD_COMMA -> "NUMPAD_COMMA"
    KeyEvent.KEYCODE_NUMPAD_DIVIDE -> "NUMPAD_DIVIDE"
    KeyEvent.KEYCODE_NUMPAD_DOT -> "NUMPAD_DOT"
    KeyEvent.KEYCODE_NUMPAD_ENTER -> "NUMPAD_ENTER"
    KeyEvent.KEYCODE_NUMPAD_EQUALS -> "NUMPAD_EQUALS"
    KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN -> "NUMPAD_LEFT_PAREN"
    KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> "NUMPAD_MULTIPLY"
    KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN -> "NUMPAD_RIGHT_PAREN"
    KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> "NUMPAD_SUBTRACT"
    KeyEvent.KEYCODE_NUM_LOCK -> "NUM_LOCK"
    KeyEvent.KEYCODE_O -> "O"
    KeyEvent.KEYCODE_P -> "P"
    KeyEvent.KEYCODE_PAGE_DOWN -> "PAGE_DOWN"
    KeyEvent.KEYCODE_PAGE_UP -> "PAGE_UP"
    KeyEvent.KEYCODE_PAIRING -> "PAIRING"
    KeyEvent.KEYCODE_PASTE -> "PASTE"
    KeyEvent.KEYCODE_PERIOD -> "PERIOD"
    KeyEvent.KEYCODE_PICTSYMBOLS -> "PICTSYMBOLS"
    KeyEvent.KEYCODE_PLUS -> "PLUS"
    KeyEvent.KEYCODE_POUND -> "POUND"
    KeyEvent.KEYCODE_POWER -> "POWER"
    KeyEvent.KEYCODE_PROFILE_SWITCH -> "PROFILE_SWITCH"
    KeyEvent.KEYCODE_PROG_BLUE -> "PROG_BLUE"
    KeyEvent.KEYCODE_PROG_GREEN -> "PROG_GREEN"
    KeyEvent.KEYCODE_PROG_RED -> "PROG_RED"
    KeyEvent.KEYCODE_PROG_YELLOW -> "PROG_YELLOW"
    KeyEvent.KEYCODE_Q -> "Q"
    KeyEvent.KEYCODE_R -> "R"
    KeyEvent.KEYCODE_REFRESH -> "REFRESH"
    KeyEvent.KEYCODE_RIGHT_BRACKET -> "RIGHT_BRACKET"
    KeyEvent.KEYCODE_RO -> "RO"
    KeyEvent.KEYCODE_S -> "S"
    KeyEvent.KEYCODE_SCROLL_LOCK -> "SCROLL_LOCK"
    KeyEvent.KEYCODE_SEARCH -> "SEARCH"
    KeyEvent.KEYCODE_SEMICOLON -> "SEMICOLON"
    KeyEvent.KEYCODE_SETTINGS -> "SETTINGS"
    KeyEvent.KEYCODE_SHIFT_LEFT -> "SHIFT_LEFT"
    KeyEvent.KEYCODE_SHIFT_RIGHT -> "SHIFT_RIGHT"
    KeyEvent.KEYCODE_SLASH -> "SLASH"
    KeyEvent.KEYCODE_SLEEP -> "SLEEP"
    KeyEvent.KEYCODE_SOFT_LEFT -> "SOFT_LEFT"
    KeyEvent.KEYCODE_SOFT_RIGHT -> "SOFT_RIGHT"
    KeyEvent.KEYCODE_SOFT_SLEEP -> "SOFT_SLEEP"
    KeyEvent.KEYCODE_SPACE -> "SPACE"
    KeyEvent.KEYCODE_STAR -> "STAR"
    KeyEvent.KEYCODE_STB_INPUT -> "STB_INPUT"
    KeyEvent.KEYCODE_STB_POWER -> "STB_POWER"
    KeyEvent.KEYCODE_STEM_1 -> "STEM_1"
    KeyEvent.KEYCODE_STEM_2 -> "STEM_2"
    KeyEvent.KEYCODE_STEM_3 -> "STEM_3"
    KeyEvent.KEYCODE_STEM_PRIMARY -> "STEM_PRIMARY"
    KeyEvent.KEYCODE_SWITCH_CHARSET -> "SWITCH_CHARSET"
    KeyEvent.KEYCODE_SYM -> "SYM"
    KeyEvent.KEYCODE_SYSRQ -> "SYSRQ"
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> "SYSTEM_NAVIGATION_DOWN"
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> "SYSTEM_NAVIGATION_LEFT"
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> "SYSTEM_NAVIGATION_RIGHT"
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> "SYSTEM_NAVIGATION_UP"
    KeyEvent.KEYCODE_T -> "T"
    KeyEvent.KEYCODE_TAB -> "TAB"
    KeyEvent.KEYCODE_THUMBS_DOWN -> "THUMBS_DOWN"
    KeyEvent.KEYCODE_THUMBS_UP -> "THUMBS_UP"
    KeyEvent.KEYCODE_TV -> "TV"
    KeyEvent.KEYCODE_TV_ANTENNA_CABLE -> "TV_ANTENNA_CABLE"
    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION -> "TV_AUDIO_DESCRIPTION"
    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN -> "TV_AUDIO_DESCRIPTION_MIX_DOWN"
    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP -> "TV_AUDIO_DESCRIPTION_MIX_UP"
    KeyEvent.KEYCODE_TV_CONTENTS_MENU -> "TV_CONTENTS_MENU"
    KeyEvent.KEYCODE_TV_DATA_SERVICE -> "TV_DATA_SERVICE"
    KeyEvent.KEYCODE_TV_INPUT -> "TV_INPUT"
    KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1 -> "TV_INPUT_COMPONENT_1"
    KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2 -> "TV_INPUT_COMPONENT_2"
    KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1 -> "TV_INPUT_COMPOSITE_1"
    KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2 -> "TV_INPUT_COMPOSITE_2"
    KeyEvent.KEYCODE_TV_INPUT_HDMI_1 -> "TV_INPUT_HDMI_1"
    KeyEvent.KEYCODE_TV_INPUT_HDMI_2 -> "TV_INPUT_HDMI_2"
    KeyEvent.KEYCODE_TV_INPUT_HDMI_3 -> "TV_INPUT_HDMI_3"
    KeyEvent.KEYCODE_TV_INPUT_HDMI_4 -> "TV_INPUT_HDMI_4"
    KeyEvent.KEYCODE_TV_INPUT_VGA_1 -> "TV_INPUT_VGA_1"
    KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU -> "TV_MEDIA_CONTEXT_MENU"
    KeyEvent.KEYCODE_TV_NETWORK -> "TV_NETWORK"
    KeyEvent.KEYCODE_TV_NUMBER_ENTRY -> "TV_NUMBER_ENTRY"
    KeyEvent.KEYCODE_TV_POWER -> "TV_POWER"
    KeyEvent.KEYCODE_TV_RADIO_SERVICE -> "TV_RADIO_SERVICE"
    KeyEvent.KEYCODE_TV_SATELLITE -> "TV_SATELLITE"
    KeyEvent.KEYCODE_TV_SATELLITE_BS -> "TV_SATELLITE_BS"
    KeyEvent.KEYCODE_TV_SATELLITE_CS -> "TV_SATELLITE_CS"
    KeyEvent.KEYCODE_TV_SATELLITE_SERVICE -> "TV_SATELLITE_SERVICE"
    KeyEvent.KEYCODE_TV_TELETEXT -> "TV_TELETEXT"
    KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG -> "TV_TERRESTRIAL_ANALOG"
    KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL -> "TV_TERRESTRIAL_DIGITAL"
    KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING -> "TV_TIMER_PROGRAMMING"
    KeyEvent.KEYCODE_TV_ZOOM_MODE -> "TV_ZOOM_MODE"
    KeyEvent.KEYCODE_U -> "U"
    KeyEvent.KEYCODE_UNKNOWN -> "UNKNOWN"
    KeyEvent.KEYCODE_V -> "V"
    KeyEvent.KEYCODE_VOICE_ASSIST -> "VOICE_ASSIST"
    KeyEvent.KEYCODE_VOLUME_DOWN -> "VOLUME_DOWN"
    KeyEvent.KEYCODE_VOLUME_MUTE -> "VOLUME_MUTE"
    KeyEvent.KEYCODE_VOLUME_UP -> "VOLUME_UP"
    KeyEvent.KEYCODE_W -> "W"
    KeyEvent.KEYCODE_WAKEUP -> "WAKEUP"
    KeyEvent.KEYCODE_WINDOW -> "WINDOW"
    KeyEvent.KEYCODE_X -> "X"
    KeyEvent.KEYCODE_Y -> "Y"
    KeyEvent.KEYCODE_YEN -> "YEN"
    KeyEvent.KEYCODE_Z -> "Z"
    KeyEvent.KEYCODE_ZENKAKU_HANKAKU -> "ZENKAKU_HANKAKU"
    KeyEvent.KEYCODE_ZOOM_IN -> "ZOOM_IN"
    KeyEvent.KEYCODE_ZOOM_OUT -> "ZOOM_OUT"
    else -> default
}

internal fun stringifyKeyAction(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    KeyEvent.ACTION_DOWN -> "DOWN"
    KeyEvent.ACTION_UP -> "UP"
    KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
    else -> default
}

internal fun stringifyDockState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    Intent.EXTRA_DOCK_STATE_CAR -> "CAR"
    Intent.EXTRA_DOCK_STATE_DESK -> "DESK"
    Intent.EXTRA_DOCK_STATE_HE_DESK -> "HE_DESK"
    Intent.EXTRA_DOCK_STATE_LE_DESK -> "LE_DESK"
    Intent.EXTRA_DOCK_STATE_UNDOCKED -> "UNDOCKED"
    else -> default
}

internal fun stringifyCallState(flag: String?, default: String = UNDEFINED): String = when (flag) {
    TelephonyManager.EXTRA_STATE_IDLE -> "IDLE"
    TelephonyManager.EXTRA_STATE_OFFHOOK -> "OFFHOOK"
    TelephonyManager.EXTRA_STATE_RINGING -> "RINGING"
    else -> default
}

internal fun stringifyWifiState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    WifiManager.WIFI_STATE_DISABLED -> "DISABLED"
    WifiManager.WIFI_STATE_DISABLING -> "DISABLING"
    WifiManager.WIFI_STATE_ENABLED -> "ENABLED"
    WifiManager.WIFI_STATE_ENABLING -> "ENABLING"
    WifiManager.WIFI_STATE_UNKNOWN -> "UNKNOWN"
    else -> default
}

internal fun stringifyHeadsetState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    0 -> "UNPLUGGED"
    1 -> "PLUGGED"
    else -> default
}

internal fun stringifyRingerMode(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
    AudioManager.RINGER_MODE_SILENT -> "SILENT"
    AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
    else -> default
}

internal fun stringifyScoAudioState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> "DISCONNECTED"
    AudioManager.SCO_AUDIO_STATE_CONNECTING -> "CONNECTING"
    AudioManager.SCO_AUDIO_STATE_CONNECTED -> "CONNECTED"
    else -> default
}

internal fun stringifyAudioPlugState(flag: Int?, default: String = UNDEFINED): String = when (flag) {
    0 -> "UNPLUGGED"
    1 -> "PLUGGED"
    else -> default
}

internal fun stringifyNetworkTransport(capabilities: NetworkCapabilities?, default: String = UNDEFINED) =
    capabilities?.let {
        listOfNotNull(
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "CELLULAR" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "WIFI" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) "BLUETOOTH" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) "ETHERNET" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) "VPN" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) "WIFI_AWARE" else null,
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) "LOWPAN" else null
        ).sorted().joinToString(";")
    }?.takeIf { it.isNotBlank() } ?: default


internal fun stringifyNotificationVisibility(flag: Int?, default: String = UNDEFINED) = when (flag) {
    Notification.VISIBILITY_PRIVATE -> "PRIVATE"
    Notification.VISIBILITY_PUBLIC -> "PUBLIC"
    Notification.VISIBILITY_SECRET -> "SECRET"
    else -> default
}

internal fun stringifyNotificationCategory(flag: String?, default: String = UNDEFINED) = when (flag) {
    Notification.CATEGORY_ALARM -> "ALARM"
    Notification.CATEGORY_CALL -> "CALL"
    Notification.CATEGORY_EMAIL -> "EMAIL"
    Notification.CATEGORY_ERROR -> "ERROR"
    Notification.CATEGORY_EVENT -> "EVENT"
    Notification.CATEGORY_MESSAGE -> "MESSAGE"
    Notification.CATEGORY_NAVIGATION -> "NAVIGATION"
    Notification.CATEGORY_PROGRESS -> "PROGRESS"
    Notification.CATEGORY_PROMO -> "PROMO"
    Notification.CATEGORY_RECOMMENDATION -> "RECOMMENDATION"
    Notification.CATEGORY_REMINDER -> "REMINDER"
    Notification.CATEGORY_SERVICE -> "SERVICE"
    Notification.CATEGORY_SOCIAL -> "SOCIAL"
    Notification.CATEGORY_STATUS -> "STATUS"
    Notification.CATEGORY_SYSTEM -> "SYSTEM"
    Notification.CATEGORY_TRANSPORT -> "TRANSPORT"
    else -> default
}

internal fun stringifyNotificationPriority(flag: Int?, default: String = UNDEFINED) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    when (flag) {
        NotificationManager.IMPORTANCE_DEFAULT -> "DEFAULT"
        NotificationManager.IMPORTANCE_HIGH -> "HIGH"
        NotificationManager.IMPORTANCE_LOW -> "LOW"
        NotificationManager.IMPORTANCE_MAX -> "MAX"
        NotificationManager.IMPORTANCE_MIN -> "MIN"
        NotificationManager.IMPORTANCE_NONE -> "NONE"
        else -> default
    }
} else {
    when (flag) {
        Notification.PRIORITY_DEFAULT -> "DEFAULT"
        Notification.PRIORITY_HIGH -> "HIGH"
        Notification.PRIORITY_LOW -> "LOW"
        Notification.PRIORITY_MAX -> "MAX"
        Notification.PRIORITY_MIN -> "MIN"
        else -> default
    }
}

internal fun stringifyFitnessDeviceType(flag: Int?, default: String = UNDEFINED) = when (flag) {
    Device.TYPE_UNKNOWN -> "UNKNOWN"
    Device.TYPE_PHONE -> "PHONE"
    Device.TYPE_TABLET -> "TABLET"
    Device.TYPE_WATCH -> "WATCH"
    Device.TYPE_CHEST_STRAP -> "CHEST_STRAP"
    Device.TYPE_SCALE -> "SCALE"
    Device.TYPE_HEAD_MOUNTED -> "HEAD_MOUNTED"
    else -> default
}

internal fun stringifySensorAccuracy(flag: Int?, default: String = UNDEFINED) = when (flag) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW"
    else -> default
}

internal fun stringifyContactType(flag: Int?, default: String = UNDEFINED): String? = when (flag) {
    ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "ASSISTANT"
    ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK -> "CALLBACK"
    ContactsContract.CommonDataKinds.Phone.TYPE_CAR -> "CAR"
    ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> "COMPANY_MAIN"
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "FAX_HOME"
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "FAX_WORK"
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
    ContactsContract.CommonDataKinds.Phone.TYPE_ISDN -> "ISDN"
    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "MAIN"
    ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "MOBILE"
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "OTHER"
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX -> "OTHER_FAX"
    ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "PAGER"
    ContactsContract.CommonDataKinds.Phone.TYPE_RADIO -> "RADIO"
    ContactsContract.CommonDataKinds.Phone.TYPE_TELEX -> "TELEX"
    ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> "TTY_TDD"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "WORK_MOBILE"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "WORK_PAGER"
    else -> default
}