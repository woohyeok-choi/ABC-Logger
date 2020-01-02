package kaist.iclab.abclogger

import org.greenrobot.eventbus.EventBus

data class ABCEvent(
        val timestamp: Long,
        val eventType: String
) {
    companion object {
        fun post(timestamp: Long, eventType: String) {
            EventBus.getDefault().post(ABCEvent(timestamp, eventType))
        }

        fun register(subscriber: Any) {
            EventBus.getDefault().register(subscriber)
        }

        fun unregister(subscriber: Any) {
            EventBus.getDefault().unregister(subscriber)
        }

        const val ACTIVITY_ENTER_IN_VEHICLE = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_IN_VEHICLE"
        const val ACTIVITY_ENTER_ON_BICYCLE = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_ON_BICYCLE"
        const val ACTIVITY_ENTER_ON_FOOT = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_ON_FOOT"
        const val ACTIVITY_ENTER_RUNNING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_RUNNING"
        const val ACTIVITY_ENTER_STILL = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_STILL"
        const val ACTIVITY_ENTER_TILTING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_TILTING"
        const val ACTIVITY_ENTER_WALKING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_ENTER_WALKING"
        const val ACTIVITY_EXIT_IN_VEHICLE = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_IN_VEHICLE"
        const val ACTIVITY_EXIT_ON_BICYCLE = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_ON_BICYCLE"
        const val ACTIVITY_EXIT_ON_FOOT = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_ON_FOOT"
        const val ACTIVITY_EXIT_RUNNING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_RUNNING"
        const val ACTIVITY_EXIT_STILL = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_STILL"
        const val ACTIVITY_EXIT_TILTING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_TILTING"
        const val ACTIVITY_EXIT_WALKING = "${BuildConfig.APPLICATION_ID}.ACTIVITY_EXIT_WALKING"
        const val HEADSET_PLUG_MICROPHONE = "${BuildConfig.APPLICATION_ID}.HEADSET_PLUG_MICROPHONE"
        const val HEADSET_UNPLUG_MICROPHONE = "${BuildConfig.APPLICATION_ID}.HEADSET_UNPLUG_MICROPHONE"
        const val HEADSET_PLUG = "${BuildConfig.APPLICATION_ID}.HEADSET_PLUG"
        const val HEADSET_UNPLUG = "${BuildConfig.APPLICATION_ID}.HEADSET_UNPLUG"
        const val POWER_CONNECTED = "${BuildConfig.APPLICATION_ID}.POWER_CONNECTED"
        const val POWER_DISCONNECTED = "${BuildConfig.APPLICATION_ID}.POWER_DISCONNECTED"
        const val SHUTDOWN = "${BuildConfig.APPLICATION_ID}.SHUTDOWN"
        const val POWER_SAVE_MODE_ACTIVATE = "${BuildConfig.APPLICATION_ID}.POWER_SAVE_MODE_ACTIVATE"
        const val POWER_SAVE_MODE_DEACTIVATE = "${BuildConfig.APPLICATION_ID}.POWER_SAVE_MODE_DEACTIVATE"
        const val SMS_DELIVERED = "${BuildConfig.APPLICATION_ID}.SMS_DELIVERED"
        const val SMS_RECEIVED = "${BuildConfig.APPLICATION_ID}.SMS_RECEIVED"
        const val DEVICE_IDLE_MODE_ACTIVATE = "${BuildConfig.APPLICATION_ID}.DEVICE_IDLE_MODE_ACTIVATE"
        const val DEVICE_IDLE_MODE_DEACTIVATE = "${BuildConfig.APPLICATION_ID}.DEVICE_IDLE_MODE_DEACTIVATE"
        const val AIRPLANE_MODE_ACTIVATE = "${BuildConfig.APPLICATION_ID}.AIRPLANE_MODE_ACTIVATE"
        const val AIRPLANE_MODE_DEACTIVATE = "${BuildConfig.APPLICATION_ID}.AIRPLANE_MODE_DEACTIVATE"
        const val CAMERA_BUTTON = "${BuildConfig.APPLICATION_ID}.CAMERA_BUTTON"
        const val USER_PRESENT = "${BuildConfig.APPLICATION_ID}.USER_PRESENT"
        const val SCREEN_ON = "${BuildConfig.APPLICATION_ID}.SCREEN_ON"
        const val SCREEN_OFF = "${BuildConfig.APPLICATION_ID}.SCREEN_OFF"
        const val RINGER_MODE_NORMAL = "${BuildConfig.APPLICATION_ID}.RINGER_MODE_NORMAL"
        const val RINGER_MODE_SILENT = "${BuildConfig.APPLICATION_ID}.RINGER_MODE_SILENT"
        const val RINGER_MODE_VIBRATE = "${BuildConfig.APPLICATION_ID}.RINGER_MODE_VIBRATE"
        const val RINGER_MODE_UNKNOWN = "${BuildConfig.APPLICATION_ID}.RINGER_MODE_UNKNOWN"
        const val BATTERY_LOW = "${BuildConfig.APPLICATION_ID}.BATTERY_LOW"
        const val BATTERY_OKAY = "${BuildConfig.APPLICATION_ID}.BATTERY_OKAY"
        const val MEDIA_BUTTON = "${BuildConfig.APPLICATION_ID}.MEDIA_BUTTON"
        const val PHONE_STATE_IDLE = "${BuildConfig.APPLICATION_ID}.PHONE_STATE_IDLE"
        const val PHONE_STATE_OFFHOOK = "${BuildConfig.APPLICATION_ID}.PHONE_STATE_OFFHOOK"
        const val PHONE_STATE_RINGING = "${BuildConfig.APPLICATION_ID}.PHONE_STATE_RINGING"
        const val NOTIFICATION_POSTED = "${BuildConfig.APPLICATION_ID}.NOTIFICATION_POSTED"
        const val NOTIFICATION_REMOVED= "${BuildConfig.APPLICATION_ID}.NOTIFICATION_REMOVED"
        const val BOOT_COMPLETED = "${BuildConfig.APPLICATION_ID}.BOOT_COMPLETED"
        const val BLUETOOTH_ACTIVATE = "${BuildConfig.APPLICATION_ID}.BLUETOOTH_ACTIVATE"
        const val BLUETOOTH_DEACTIVATE = "${BuildConfig.APPLICATION_ID}.BLUETOOTH_DEACTIVATE"
    }
}

data class ErrorEvent(val timestamp: Long,
                      val className: String,
                      val message: String) {
    companion object {
        fun post(timestamp: Long, eventType: String) {
            EventBus.getDefault().post(ErrorEvent(timestamp, eventType))
        }

        fun register(subscriber: Any) {
            EventBus.getDefault().register(subscriber)
        }

        fun unregister(subscriber: Any) {
            EventBus.getDefault().unregister(subscriber)
        }
    }
}