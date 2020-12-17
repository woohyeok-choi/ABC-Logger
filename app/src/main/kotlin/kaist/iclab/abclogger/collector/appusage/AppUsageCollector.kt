package kaist.iclab.abclogger.collector.appusage

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.stringifyAppUsageEvent
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.util.concurrent.TimeUnit

class AppUsageCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<AppUsageEventEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    private val alarmManager by lazy {
        context.getSystemService<AlarmManager>()!!
    }

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_APP_USAGE,
            Intent(ACTION_RETRIEVE_APP_USAGE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_RETRIEVE_APP_USAGE) return
            handleRetrieval()
        }
    }

    override fun getDescription(): Array<Description> = arrayOf()

    override fun isAvailable(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_RETRIEVE_APP_USAGE)
        })

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20),
            TimeUnit.MINUTES.toMillis(30),
            intent
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<AppUsageEventEntity>()

    override suspend fun flush(entities: Collection<AppUsageEventEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<AppUsageEventEntity> = dataRepository.find(0, limit)

    private fun handleRetrieval() = launch {
        val toTime = System.currentTimeMillis()
        val fromTime = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeDataWritten
        )
        val usageStatManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val events = usageStatManager.queryEvents(fromTime, toTime)
        val event = UsageEvents.Event()
        val entities = mutableListOf<AppUsageEventEntity>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val entity = AppUsageEventEntity(
                name = getApplicationName(packageManager, event.packageName)
                    ?: "",
                packageName = event.packageName ?: "",
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    stringifyAppUsageEvent(event.eventType, event.appStandbyBucket)
                } else {
                    stringifyAppUsageEvent(event.eventType)
                },
                isSystemApp = isSystemApp(packageManager, event.packageName),
                isUpdatedSystemApp = isUpdatedSystemApp(packageManager, event.packageName)
            ).apply {
                timestamp = event.timeStamp
            }
            entities.add(entity)
        }

        entities.forEach {
            put(it)
        }

    }

    companion object {
        private const val ACTION_RETRIEVE_APP_USAGE =
            "${BuildConfig.APPLICATION_ID}.ACTION_RETRIEVE_APP_USAGE"
        private const val REQUEST_CODE_APP_USAGE = 0xee
    }
}