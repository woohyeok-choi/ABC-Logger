package kaist.iclab.abclogger.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kaist.iclab.abclogger.BatteryEntity
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.fillBaseInfo

class BatteryCollector (val context: Context) : BaseCollector {
    private val receiver : BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return

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
                ).fillBaseInfo(
                        timeMillis = System.currentTimeMillis()
                ).run {
                    putEntity(this)
                }
            }
        }
    }

    private val filter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    override fun onStart() {
        context.registerReceiver(receiver, filter)
    }

    override fun onStop() {
        context.unregisterReceiver(receiver)
    }

    override fun checkAvailability(): Boolean = true

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

}