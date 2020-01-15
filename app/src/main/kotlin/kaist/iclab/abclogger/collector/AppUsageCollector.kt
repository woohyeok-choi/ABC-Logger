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
import android.os.Build
import android.provider.Settings
import android.os.Process
import androidx.core.app.AppOpsManagerCompat
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector


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

    private val filter: IntentFilter = IntentFilter().apply {
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

    private fun handleRetrieveAppUsage() {
        val timestamp = System.currentTimeMillis()

        if (SharedPrefs.lastAccessTimeAppUsage < 0) {
            SharedPrefs.lastAccessTimeAppUsage = timestamp
            return
        }

        val events = usageStatManager.queryEvents(SharedPrefs.lastAccessTimeAppUsage, timestamp)
        val event = UsageEvents.Event()
        val entities = mutableListOf<AppUsageEventEntity>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val entity = AppUsageEventEntity(
                    name = getApplicationName(packageManager = packageManager, packageName = event.packageName) ?: "",
                    packageName = event.packageName,
                    type = eventTypeToString(event.eventType),
                    isSystemApp = isSystemApp(packageManager = packageManager, packageName = event.packageName),
                    isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = event.packageName)
            )
            entities.add(entity)
        }
        putEntity(entities)
        SharedPrefs.lastAccessTimeAppUsage = timestamp
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_RETRIEVE_APP_USAGE) return

            handleRetrieveAppUsage()
        }
    }

    override fun onStart() {
        val currentTime = System.currentTimeMillis()
        val threeHour: Long = 1000 * 60 * 60 * 3

        val triggerTime: Long = if (SharedPrefs.lastAccessTimeAppUsage < 0 ||
                SharedPrefs.lastAccessTimeAppUsage + threeHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            SharedPrefs.lastAccessTimeAppUsage + threeHour
        }

        context.registerReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, threeHour, intent)
    }

    override fun onStop() {
        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    override fun checkAvailability(): Boolean {
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

    companion object {
        private const val ACTION_RETRIEVE_APP_USAGE = "${BuildConfig.APPLICATION_ID}.ACTION_RETRIEVE_APP_USAGE"
        private const val REQUEST_CODE_APP_USAGE = 0xee
    }
}