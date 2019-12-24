package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.WifiEntity
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.fillBaseInfo

class WifiCollector(val context: Context) : BaseCollector {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val alarmManager: AlarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val intent: PendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_WIFI_SCAN,
            Intent(ACTION_WIFI_SCAN), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_WIFI_SCAN)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timestamp = System.currentTimeMillis()
            wifiManager.scanResults.map { result ->
                WifiEntity(
                        bssid = result.BSSID,
                        ssid = result.SSID,
                        frequency = result.frequency,
                        rssi = result.level
                ).fillBaseInfo(timestamp = timestamp)
            }.run {
                putEntity(this)
            }
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedWiFi || !checkAvailability()) return
        context.registerReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                1000 * 60 * 5,
                intent
        )
    }

    override fun stop() {
        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun newIntentForSetup(): Intent? = null

    companion object {
        private const val REQUEST_CODE_WIFI_SCAN = 0xf0
        private const val ACTION_WIFI_SCAN = "kaist.iclab.abclogger.ACTION_WIFI_SCAN"
    }
}