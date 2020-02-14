package kaist.iclab.abclogger.collector.event

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.TelephonyManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.AbcEvent
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class DeviceEventCollector(private val context: Context) : BaseCollector<DeviceEventCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_device_event)

    override val description: String = context.getString(R.string.data_desc_device_event)

    override val requiredPermissions: List<String> = listOf(
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH
    )

    override val newIntentForSetUp: Intent? = null

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, filter)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
    }

    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val filter = arrayOf(
            Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
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
            Intent.ACTION_MEDIA_BUTTON,
            TelephonyManager.ACTION_PHONE_STATE_CHANGED,
            Telephony.Sms.Intents.SMS_DELIVER_ACTION,
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION,
            BluetoothAdapter.ACTION_STATE_CHANGED
    ).let { intents ->
        IntentFilter().apply { intents.forEach { addAction(it) } }
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                handleEventRetrieval(intent)
            }
        }
    }

    private fun handleEventRetrieval(intent: Intent) {
        val timestamp = System.currentTimeMillis()

        val eventType = when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val isPlugged = intent.getIntExtra("state", 0) == 1
                val hasMicrophone = intent.getIntExtra("microphone", 0) == 1

                if (isPlugged && hasMicrophone) {
                    AbcEvent.HEADSET_PLUG_MICROPHONE
                } else if (!isPlugged && hasMicrophone) {
                    AbcEvent.HEADSET_UNPLUG_MICROPHONE
                } else if (isPlugged && !hasMicrophone) {
                    AbcEvent.HEADSET_PLUG
                } else {
                    AbcEvent.HEADSET_UNPLUG
                }
            }
            Intent.ACTION_POWER_CONNECTED -> AbcEvent.POWER_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> AbcEvent.POWER_DISCONNECTED
            Intent.ACTION_SHUTDOWN -> AbcEvent.SHUTDOWN
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                if (powerManager.isPowerSaveMode) AbcEvent.POWER_SAVE_MODE_ACTIVATE
                else AbcEvent.POWER_SAVE_MODE_DEACTIVATE
            PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED ->
                if (powerManager.isDeviceIdleMode) AbcEvent.DEVICE_IDLE_MODE_ACTIVATE
                else AbcEvent.DEVICE_IDLE_MODE_DEACTIVATE
            Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                if (intent.getBooleanExtra("state", false)) AbcEvent.AIRPLANE_MODE_ACTIVATE
                else AbcEvent.AIRPLANE_MODE_DEACTIVATE
            Intent.ACTION_CAMERA_BUTTON -> AbcEvent.CAMERA_BUTTON
            Intent.ACTION_USER_PRESENT -> AbcEvent.USER_PRESENT
            Intent.ACTION_SCREEN_ON -> AbcEvent.SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> AbcEvent.SCREEN_OFF
            AudioManager.RINGER_MODE_CHANGED_ACTION ->
                when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)) {
                    AudioManager.RINGER_MODE_NORMAL -> AbcEvent.RINGER_MODE_NORMAL
                    AudioManager.RINGER_MODE_SILENT -> AbcEvent.RINGER_MODE_SILENT
                    AudioManager.RINGER_MODE_VIBRATE -> AbcEvent.RINGER_MODE_VIBRATE
                    else -> AbcEvent.RINGER_MODE_UNKNOWN
                }
            Intent.ACTION_BATTERY_LOW -> AbcEvent.BATTERY_LOW
            Intent.ACTION_BATTERY_OKAY -> AbcEvent.BATTERY_OKAY
            Intent.ACTION_MEDIA_BUTTON -> AbcEvent.MEDIA_BUTTON
            TelephonyManager.ACTION_PHONE_STATE_CHANGED ->
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_IDLE -> AbcEvent.PHONE_STATE_IDLE
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> AbcEvent.PHONE_STATE_OFFHOOK
                    TelephonyManager.EXTRA_STATE_RINGING -> AbcEvent.PHONE_STATE_RINGING
                    else -> null
                }
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> AbcEvent.SMS_DELIVERED
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> AbcEvent.SMS_RECEIVED
            BluetoothAdapter.ACTION_STATE_CHANGED ->
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF) == BluetoothAdapter.STATE_OFF) AbcEvent.BLUETOOTH_DEACTIVATE
                else AbcEvent.BLUETOOTH_ACTIVATE
            else -> null
        } ?: return

        AbcEvent.post(timestamp = timestamp, eventType = eventType)

        DeviceEventEntity(
                type = eventType
        ).fill(timeMillis = timestamp).also { entity ->
            launch {
                ObjBox.put(entity)
                setStatus(Status(lastTime = timestamp))
            }
        }
    }
}