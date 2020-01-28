package kaist.iclab.abclogger.collector.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BatteryCollector(val context: Context) : BaseCollector {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val timestamp = System.currentTimeMillis()

                BatteryEntity(
                        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
                        temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1),
                        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1),
                        health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                            BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
                            else -> "UNKNOWN"
                        },
                        pluggedType = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                            else -> "UNKNOWN"
                        },
                        status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                            BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
                            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
                            BatteryManager.BATTERY_STATUS_FULL -> "FULL"
                            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
                            else -> "UNKNOWN"
                        }
                ).fill(timeMillis = timestamp).also { entity ->
                    GlobalScope.launch {
                        ObjBox.put(entity)
                        setStatus(Status(lastTime = timestamp))
                    }
                }
            }
        }
    }

    private val filter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, filter)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
    }

    override suspend fun checkAvailability(): Boolean = true

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

}