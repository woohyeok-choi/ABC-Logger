package kaist.iclab.abclogger.collector.wifi

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.provider.Settings
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WifiCollector(val context: Context) : BaseCollector {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val alarmManager: AlarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                GlobalScope.launch { handleWifiUpdate() }
            }
        }
    }

    private val intent: PendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_WIFI_SCAN,
            Intent(ACTION_WIFI_SCAN), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_WIFI_SCAN)
        addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
    }

    private suspend fun handleWifiUpdate() {
        val timestamp = System.currentTimeMillis()

        try {
            wifiManager.scanResults.map { result ->
                WifiEntity(
                        bssid = result.BSSID,
                        ssid = result.SSID,
                        frequency = result.frequency,
                        rssi = result.level
                ).fill(timeMillis = timestamp)
            }.also { entity ->
                ObjBox.put(entity)
                setStatus(Status(lastTime = timestamp))
            }
        } catch (e: SecurityException) { }
    }

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5000,
                TimeUnit.MINUTES.toMillis(5),
                intent
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intent)
    }

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    companion object {
        private const val REQUEST_CODE_WIFI_SCAN = 0xf0
        private const val ACTION_WIFI_SCAN = "${BuildConfig.APPLICATION_ID}.ACTION_WIFI_SCAN"
    }
}