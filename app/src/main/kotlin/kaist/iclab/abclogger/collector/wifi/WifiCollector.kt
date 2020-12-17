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
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.util.concurrent.TimeUnit

class WifiCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<WifiEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleWifiUpdate()
        }
    }

    private val intent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_WIFI_SCAN,
            Intent(ACTION_WIFI_SCAN), PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override val permissions: List<String> = listOf(
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override val setupIntent: Intent? = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf()

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_WIFI_SCAN)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        })

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


    override suspend fun count(): Long = dataRepository.count<WifiEntity>()

    override suspend fun flush(entities: Collection<WifiEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<WifiEntity> = dataRepository.find(0, limit)

    private fun handleWifiUpdate() = launch {
        val timestamp = System.currentTimeMillis()
        val accessPoints = wifiManager.scanResults.map { result ->
            WifiEntity.AccessPoint(
                bssid = result.BSSID,
                ssid = result.SSID,
                frequency = result.frequency,
                rssi = result.level
            )
        }
        put(
            WifiEntity(accessPoints = accessPoints).apply {
                this.timestamp = timestamp
            }
        )
    }

    companion object {
        private const val REQUEST_CODE_WIFI_SCAN = 0xf0
        private const val ACTION_WIFI_SCAN = "${BuildConfig.APPLICATION_ID}.ACTION_WIFI_SCAN"
    }
}