package kaist.iclab.abclogger.collector.install

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.getApplicationName
import kaist.iclab.abclogger.collector.isSystemApp
import kaist.iclab.abclogger.collector.isUpdatedSystemApp
import java.util.concurrent.TimeUnit

class InstalledAppCollector (val context: Context) : BaseCollector {
    private val packageManager: PackageManager by lazy { context.applicationContext.packageManager }

    private val alarmManager: AlarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_RETRIEVE_PACKAGES) return
                val timestamp = System.currentTimeMillis()

                packageManager.getInstalledPackages(
                        PackageManager.GET_META_DATA
                ).map { info ->
                    InstalledAppEntity(
                            name = getApplicationName(packageManager = packageManager, packageName = info.packageName)
                                    ?: "",
                            packageName = info.packageName,
                            isSystemApp = isSystemApp(packageManager = packageManager, packageName = info.packageName),
                            isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = info.packageName),
                            firstInstallTime = info.firstInstallTime,
                            lastUpdateTime = info.lastUpdateTime
                    ).fill(timeMillis = timestamp)
                }.run {
                    ObjBox.put(this)
                }

                CollectorPrefs.lastAccessTimeInstalledApp = timestamp
            }
        }
    }

    private val intent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_RETRIEVE_PACKAGES,
            Intent(ACTION_RETRIEVE_PACKAGES), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_RETRIEVE_PACKAGES)
    }

    override suspend fun onStart() {
        val currentTime = System.currentTimeMillis()
        val halfDayHour : Long = TimeUnit.HOURS.toMillis(12)

        val triggerTime : Long = if(CollectorPrefs.lastAccessTimeInstalledApp < 0 ||
                CollectorPrefs.lastAccessTimeInstalledApp + halfDayHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            CollectorPrefs.lastAccessTimeInstalledApp + halfDayHour
        }

        context.safeRegisterReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                halfDayHour,
                intent
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean = true

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null


    companion object {
        private const val ACTION_RETRIEVE_PACKAGES = "${BuildConfig.APPLICATION_ID}.ACTION_RETRIEVE_PACKAGES"
        private const val REQUEST_CODE_RETRIEVE_PACKAGES = 0xef
    }
}