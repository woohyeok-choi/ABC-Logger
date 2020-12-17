package kaist.iclab.abclogger.collector.install

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.util.concurrent.TimeUnit

/**
 * Collect installed apps every three hours.
 */
class InstalledAppCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<InstalledAppEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = null

    private val alarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val packageManager by lazy { context.packageManager }

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_RETRIEVE_PACKAGES,
            Intent(ACTION_RETRIEVE_PACKAGES), PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_RETRIEVE_PACKAGES) return
            handleScanRequest()
        }
    }

    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf()

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_RETRIEVE_PACKAGES)
        })

        val leastTriggerTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20)
        val scheduledTriggerTime = lastTimeDataWritten + TimeUnit.HOURS.toMillis(3)
        val realTriggerTime = scheduledTriggerTime.coerceAtLeast(leastTriggerTime)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            realTriggerTime,
            TimeUnit.HOURS.toMillis(3),
            intent
        )

    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<InstalledAppEntity>()

    override suspend fun flush(entities: Collection<InstalledAppEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<InstalledAppEntity> =
        dataRepository.find(0, limit)

    private fun handleScanRequest() = launch {
        val timestamp = System.currentTimeMillis()
        val apps = packageManager.getInstalledPackages(
            PackageManager.GET_META_DATA
        ).map { info ->
            InstalledAppEntity.App(
                name = getApplicationName(
                    packageManager = packageManager,
                    packageName = info.packageName
                ) ?: "",
                packageName = info.packageName ?: "",
                isSystemApp = isSystemApp(
                    packageManager = packageManager, packageName = info.packageName
                ),
                isUpdatedSystemApp = isUpdatedSystemApp(
                    packageManager = packageManager, packageName = info.packageName
                ),
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime
            )
        }
        put(
            InstalledAppEntity(apps = apps).apply {
                this.timestamp = timestamp
            }
        )
    }

    companion object {
        private const val ACTION_RETRIEVE_PACKAGES =
            "${BuildConfig.APPLICATION_ID}.ACTION_RETRIEVE_PACKAGES"
        private const val REQUEST_CODE_RETRIEVE_PACKAGES = 0xef
    }
}
