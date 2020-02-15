package kaist.iclab.abclogger

import org.greenrobot.eventbus.EventBus

data class AbcEvent(
        val timestamp: Long,
        val eventType: String
) {
    companion object {
        fun post(timestamp: Long, eventType: String) {
            EventBus.getDefault().post(AbcEvent(timestamp, eventType))
        }

        fun register(subscriber: Any) {
            EventBus.getDefault().register(subscriber)
        }

        fun unregister(subscriber: Any) {
            EventBus.getDefault().unregister(subscriber)
        }

        const val ACTIVITY_ENTER_IN_VEHICLE = "ACTIVITY_ENTER_IN_VEHICLE"
        const val ACTIVITY_ENTER_ON_BICYCLE = "ACTIVITY_ENTER_ON_BICYCLE"
        const val ACTIVITY_ENTER_ON_FOOT = "ACTIVITY_ENTER_ON_FOOT"
        const val ACTIVITY_ENTER_RUNNING = "ACTIVITY_ENTER_RUNNING"
        const val ACTIVITY_ENTER_STILL = "ACTIVITY_ENTER_STILL"
        const val ACTIVITY_ENTER_TILTING = "ACTIVITY_ENTER_TILTING"
        const val ACTIVITY_ENTER_WALKING = "ACTIVITY_ENTER_WALKING"
        const val ACTIVITY_EXIT_IN_VEHICLE = "ACTIVITY_EXIT_IN_VEHICLE"
        const val ACTIVITY_EXIT_ON_BICYCLE = "ACTIVITY_EXIT_ON_BICYCLE"
        const val ACTIVITY_EXIT_ON_FOOT = "ACTIVITY_EXIT_ON_FOOT"
        const val ACTIVITY_EXIT_RUNNING = "ACTIVITY_EXIT_RUNNING"
        const val ACTIVITY_EXIT_STILL = "ACTIVITY_EXIT_STILL"
        const val ACTIVITY_EXIT_TILTING = "ACTIVITY_EXIT_TILTING"
        const val ACTIVITY_EXIT_WALKING = "ACTIVITY_EXIT_WALKING"
        const val HEADSET_PLUG_MICROPHONE = "HEADSET_PLUG_MICROPHONE"
        const val HEADSET_UNPLUG_MICROPHONE = "HEADSET_UNPLUG_MICROPHONE"
        const val HEADSET_PLUG = "HEADSET_PLUG"
        const val HEADSET_UNPLUG = "HEADSET_UNPLUG"
        const val POWER_CONNECTED = "POWER_CONNECTED"
        const val POWER_DISCONNECTED = "POWER_DISCONNECTED"
        const val SHUTDOWN = "SHUTDOWN"
        const val POWER_SAVE_MODE_ACTIVATE = "POWER_SAVE_MODE_ACTIVATE"
        const val POWER_SAVE_MODE_DEACTIVATE = "POWER_SAVE_MODE_DEACTIVATE"
        const val SMS_DELIVERED = "SMS_DELIVERED"
        const val SMS_RECEIVED = "SMS_RECEIVED"
        const val DEVICE_IDLE_MODE_ACTIVATE = "DEVICE_IDLE_MODE_ACTIVATE"
        const val DEVICE_IDLE_MODE_DEACTIVATE = "DEVICE_IDLE_MODE_DEACTIVATE"
        const val AIRPLANE_MODE_ACTIVATE = "AIRPLANE_MODE_ACTIVATE"
        const val AIRPLANE_MODE_DEACTIVATE = "AIRPLANE_MODE_DEACTIVATE"
        const val CAMERA_BUTTON = "CAMERA_BUTTON"
        const val USER_PRESENT = "USER_PRESENT"
        const val SCREEN_ON = "SCREEN_ON"
        const val SCREEN_OFF = "SCREEN_OFF"
        const val RINGER_MODE_NORMAL = "RINGER_MODE_NORMAL"
        const val RINGER_MODE_SILENT = "RINGER_MODE_SILENT"
        const val RINGER_MODE_VIBRATE = "RINGER_MODE_VIBRATE"
        const val RINGER_MODE_UNKNOWN = "RINGER_MODE_UNKNOWN"
        const val BATTERY_LOW = "BATTERY_LOW"
        const val BATTERY_OKAY = "BATTERY_OKAY"
        const val MEDIA_BUTTON = "MEDIA_BUTTON"
        const val PHONE_STATE_IDLE = "PHONE_STATE_IDLE"
        const val PHONE_STATE_OFFHOOK = "PHONE_STATE_OFFHOOK"
        const val PHONE_STATE_RINGING = "PHONE_STATE_RINGING"
        const val NOTIFICATION_POSTED = "NOTIFICATION_POSTED"
        const val NOTIFICATION_REMOVED = "NOTIFICATION_REMOVED"
        const val BOOT_COMPLETED = "BOOT_COMPLETED"
        const val BLUETOOTH_ACTIVATE = "BLUETOOTH_ACTIVATE"
        const val BLUETOOTH_DEACTIVATE = "BLUETOOTH_DEACTIVATE"
    }
}
