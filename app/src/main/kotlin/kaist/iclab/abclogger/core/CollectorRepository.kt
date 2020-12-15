package kaist.iclab.abclogger.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.commons.isServiceRunning
import org.koin.android.ext.android.inject
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.collector.Status

@Suppress("UNCHECKED_CAST")
class CollectorRepository(vararg collector: AbstractCollector<*>) {
    private val collectors = collector

    val permissions by lazy {
        collectors.map { it.permissions }.flatten().toSet()
    }

    inline fun <reified T: AbstractCollector<*>> get() = all.filterIsInstance<T>().firstOrNull()

    operator fun get(qualifiedName: String) = collectors.firstOrNull { it.qualifiedName == qualifiedName }

    val all = collectors.toList()

    fun clear() = all.forEach { it.clear() }

    fun restart() = collectors.forEach {
        if (it.getStatus() != Status.Off) {
            it.start()
        } else {
            it.stop()
        }
    }

    fun stop(context: Context) {
        collectors.forEach { it.stop() }
        context.stopService(Intent(context, ForegroundService::class.java))
    }

    suspend fun nLocalRecords() = all.sumOf { it.count() }

    fun nRecordsUploaded() = all.sumOf { it.recordsUploaded }

    class ForegroundService : Service() {
        private val collectorRepository: CollectorRepository by inject()

        override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
            collectorRepository.restart()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            NotificationRepository.notifyForeground(
                this,
                collectorRepository.nRecordsUploaded()
            )
            collectorRepository.get<DeviceEventCollector>()?.writeBootEvent(intent)

            return START_STICKY
        }
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            restart(context, System.currentTimeMillis(), intent.action)
        }
    }

    companion object {
        internal fun restart(context: Context, timestamp: Long, action: String? = null) {
            if (isServiceRunning<ForegroundService>(context)) return

            val intent = DeviceEventCollector.fillBootEvent(
                    Intent(context, ForegroundService::class.java), timestamp, action
            )
            ContextCompat.startForegroundService(context, intent)
        }

        @SuppressLint("BatteryLife")
        fun getIgnoreBatteryOptimizationIntent(context: Context) = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:${context.packageName}")
        }

        fun getSettingIntent(context: Context) = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun getBackgroundLocationPermission() = Manifest.permission.ACCESS_BACKGROUND_LOCATION

        fun isBatteryOptimizationIgnored(context: Context) =
            context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == true

        @RequiresApi(Build.VERSION_CODES.Q)
        fun isBackgroundLocationAccessGranted(context: Context) =
                isPermissionGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}
