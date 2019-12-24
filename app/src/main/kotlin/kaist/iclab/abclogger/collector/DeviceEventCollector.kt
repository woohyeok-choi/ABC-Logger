package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.PowerManager
import kaist.iclab.abclogger.DeviceEventEntity
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.fillBaseInfo

class DeviceEventCollector(val context: Context) : BaseCollector {
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val filter = arrayOf(Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_SHUTDOWN,
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
            PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
            Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            Intent.ACTION_CAMERA_BUTTON,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
            AudioManager.RINGER_MODE_CHANGED_ACTION,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_MEDIA_BUTTON
    ).let { intents ->
        IntentFilter().apply { intents.forEach { addAction(it) } }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timestamp = System.currentTimeMillis()

            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val isPlugged = intent.getIntExtra("state", 0) == 1
                    val hasMicrophone = intent.getIntExtra("microphone", 0) == 1

                    if (isPlugged && hasMicrophone) {
                        "HEADSET_PLUG_MICROPHONE"
                    } else if (!isPlugged && hasMicrophone) {
                        "HEADSET_UNPLUG_MICROPHONE"
                    } else if (isPlugged && !hasMicrophone) {
                        "HEADSET_PLUG"
                    } else {
                        "HEADSET_UNPLUG"
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> "POWER_CONNECTED"
                Intent.ACTION_POWER_DISCONNECTED -> "POWER_DISCONNECTED"
                Intent.ACTION_BOOT_COMPLETED -> "BOOT_COMPLETED"
                Intent.ACTION_SHUTDOWN -> "SHUTDOWN"
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                    if (powerManager.isPowerSaveMode) "ACTIVATE_POWER_SAVE_MODE"
                    else "DEACTIVATE_POWER_SAVE_MODE"
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED ->
                    if (powerManager.isDeviceIdleMode) "ACTIVATE_IDLE_MODE"
                    else "DEACTIVATE_IDLE_MODE"
                Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                    if (intent.getBooleanExtra("state", false)) "ACTIVATE_AIRPLANE_MODE"
                    else "DEACTIVATE_AIRPLANE_MODE"
                Intent.ACTION_CAMERA_BUTTON -> "CAMERA_BUTTON"
                Intent.ACTION_USER_PRESENT -> "USER_PRESENT"
                Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
                Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
                AudioManager.RINGER_MODE_CHANGED_ACTION ->
                    when(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)) {
                        AudioManager.RINGER_MODE_NORMAL -> "RINGER_MODE_NORMAL"
                        AudioManager.RINGER_MODE_SILENT-> "RINGER_MODE_SILENT"
                        AudioManager.RINGER_MODE_VIBRATE -> "RINGER_MODE_VIBRATE"
                        else -> "RINGER_MODE_UNKNOWN"
                    }
                Intent.ACTION_BATTERY_LOW -> "BATTERY_LOW"
                Intent.ACTION_BATTERY_OKAY -> "BATTERY_OKAY"
                Intent.ACTION_MEDIA_BUTTON -> "MEDIA_BUTTON"
                else -> null
            }?.let {  type ->
                DeviceEventEntity(type = type).fillBaseInfo(timestamp = timestamp)
            }?.run {
                putEntity(this)
            }
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedDeviceEvent || !checkAvailability()) return

        context.registerReceiver(receiver, filter)
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedDeviceEvent || !checkAvailability()) return

        context.unregisterReceiver(receiver)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.RECEIVE_BOOT_COMPLETED
    )

    override fun newIntentForSetup(): Intent? = null
}