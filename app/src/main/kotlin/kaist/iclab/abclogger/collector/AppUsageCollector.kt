package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kaist.iclab.abclogger.AppUsageEventEntity
import kaist.iclab.abclogger.SharedPrefs
import android.provider.Settings
import android.os.Process



class AppUsageCollector(val context: Context) : BaseCollector {
    private val usageStatManager: UsageStatsManager by lazy {
        context.applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val alarmManager: AlarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val packageManager: PackageManager by lazy {
        context.applicationContext.packageManager
    }

    private val intent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_APP_USAGE,
            Intent(ACTION_RETRIEVE_APP_USAGE), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter : IntentFilter = IntentFilter().apply {
        addAction(ACTION_RETRIEVE_APP_USAGE)
    }

    private fun eventTypeToString(typeInt: Int) = when (typeInt) {
        UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED"
        UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED"
        UsageEvents.Event.ACTIVITY_STOPPED -> "ACTIVITY_STOPPED"
        UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
        UsageEvents.Event.DEVICE_SHUTDOWN -> "DEVICE_SHUTDOWN"
        UsageEvents.Event.DEVICE_STARTUP -> "DEVICE_STARTUP"
        UsageEvents.Event.FOREGROUND_SERVICE_START -> "FOREGROUND_SERVICE_START"
        UsageEvents.Event.FOREGROUND_SERVICE_STOP -> "FOREGROUND_SERVICE_STOP"
        UsageEvents.Event.KEYGUARD_HIDDEN -> "KEYGUARD_HIDDEN"
        UsageEvents.Event.KEYGUARD_SHOWN -> "KEYGUARD_SHOWN"
        UsageEvents.Event.SCREEN_INTERACTIVE -> "SCREEN_INTERACTIVE"
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "SCREEN_NON_INTERACTIVE"
        UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
        UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "STANDBY_BUCKET_CHANGED"
        UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
        else -> "NONE"
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_RETRIEVE_APP_USAGE) return

            val timestamp = System.currentTimeMillis()

            if (SharedPrefs.lastAppUsageAccessTime < 0) {
                SharedPrefs.lastAppUsageAccessTime = timestamp
                return
            }

            val events = usageStatManager.queryEvents(SharedPrefs.lastAppUsageAccessTime, timestamp)
            val event = UsageEvents.Event()
            val entities = mutableListOf<AppUsageEventEntity>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                val entity = AppUsageEventEntity(
                        name = getApplicationName(packageManager = packageManager, packageName = event.packageName)
                                ?: "",
                        packageName = event.packageName,
                        type = eventTypeToString(event.eventType),
                        isSystemApp = isSystemApp(packageManager = packageManager, packageName = event.packageName),
                        isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = event.packageName)
                )
                entities.add(entity)
            }
            putEntity(entities)
            SharedPrefs.lastAppUsageAccessTime = timestamp
        }
    }


    override fun start() {
        if (!SharedPrefs.isProvidedAppUsage || !checkAvailability()) return

        val currentTime = System.currentTimeMillis()
        val threeHour: Long = 1000 * 60 * 60 * 3

        val triggerTime : Long = if(SharedPrefs.lastAppUsageAccessTime < 0 ||
                SharedPrefs.lastAppUsageAccessTime + threeHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            SharedPrefs.lastAppUsageAccessTime + threeHour
        }

        context.registerReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                threeHour,
                intent
        )
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedAppUsage || !checkAvailability()) return

        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getRequiredPermissions(): List<String> = listOf(Manifest.permission.PACKAGE_USAGE_STATS)

    override fun newIntentForSetup(): Intent? = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    companion object {
        private const val ACTION_RETRIEVE_APP_USAGE = "kaist.iclab.abclogger.ACTION_RETRIEVE_APP_USAGE"
        private const val REQUEST_CODE_APP_USAGE = 0xee
    }
}