package kaist.iclab.abclogger.collector

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kaist.iclab.abclogger.InstalledAppEntity
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.fillBaseInfo

class InstalledAppCollector (val context: Context) : BaseCollector {
    private val packageManager: PackageManager by lazy { context.applicationContext.packageManager }

    private val alarmManager: AlarmManager by lazy {
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val intent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_RETRIEVE_PACKAGES,
            Intent(ACTION_RETRIEVE_PACKAGES), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_RETRIEVE_PACKAGES)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_RETRIEVE_PACKAGES) return
            val timestamp = System.currentTimeMillis()

            packageManager.getInstalledPackages(
                    PackageManager.GET_META_DATA
            ).map { info ->
                InstalledAppEntity(
                        name = getApplicationName(packageManager = packageManager, packageName = info.packageName) ?: "",
                        packageName = info.packageName,
                        isSystemApp = isSystemApp(packageManager = packageManager, packageName = info.packageName),
                        isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = info.packageName),
                        firstInstallTime = info.firstInstallTime,
                        lastUpdateTime = info.lastUpdateTime
                ).fillBaseInfo(timestamp = timestamp)
            }.run {
                putEntity(this)
            }

            SharedPrefs.lastInstalledAppAccessTime = timestamp
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedInstallApp || !checkAvailability()) return

        val currentTime = System.currentTimeMillis()
        val halfDayHour : Long = 1000 * 60 * 60 * 12

        val triggerTime : Long = if(SharedPrefs.lastInstalledAppAccessTime < 0 ||
                SharedPrefs.lastInstalledAppAccessTime + halfDayHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            SharedPrefs.lastInstalledAppAccessTime + halfDayHour
        }

        context.registerReceiver(receiver, filter)

        alarmManager.cancel(intent)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                halfDayHour,
                intent
        )
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedInstallApp || !checkAvailability()) return

        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean = true

    override fun getRequiredPermissions(): List<String> = listOf()

    override fun newIntentForSetup(): Intent? = null

    companion object {
        private const val ACTION_RETRIEVE_PACKAGES = "kaist.iclab.abclogger.ACTION_RETRIEVE_PACKAGES"
        private const val REQUEST_CODE_RETRIEVE_PACKAGES = 0xef
    }
}