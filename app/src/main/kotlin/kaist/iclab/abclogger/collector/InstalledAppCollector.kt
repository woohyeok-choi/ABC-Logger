package kaist.iclab.abclogger.collector

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kaist.iclab.abclogger.InstalledAppEntity
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.fillBaseInfo

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
                    ).fillBaseInfo(timeMillis = timestamp)
                }.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeInstalledApp = timestamp
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

    override fun start() {
        val currentTime = System.currentTimeMillis()
        val halfDayHour : Long = 1000 * 60 * 60 * 12

        val triggerTime : Long = if(SharedPrefs.lastAccessTimeInstalledApp < 0 ||
                SharedPrefs.lastAccessTimeInstalledApp + halfDayHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            SharedPrefs.lastAccessTimeInstalledApp + halfDayHour
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
        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean = true

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

    override val nameRes: Int?
        get() = R.string.data_name_installed_app

    override val descriptionRes: Int?
        get() = R.string.data_desc_installed_app

    companion object {
        private const val ACTION_RETRIEVE_PACKAGES = "kaist.iclab.abclogger.ACTION_RETRIEVE_PACKAGES"
        private const val REQUEST_CODE_RETRIEVE_PACKAGES = 0xef
    }
}