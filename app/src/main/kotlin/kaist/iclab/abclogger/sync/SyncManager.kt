package kaist.iclab.abclogger.sync

import android.app.Activity
import android.app.ActivityManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import io.objectbox.Box
import io.objectbox.EntityInfo
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.Base


object SyncManager {
    val TAG: String = SyncManager::class.java.simpleName

    private const val LIMIT: Long = 60
    private const val THREE_DAYS_IN_MS: Long = 1000 * 60 * 60 * 24 * 3
    private const val TEN_DAYS_IN_MS: Long = 1000 * 60 * 60 * 24 * 10
    private const val TWELVE_HOURS_IN_MS: Long = 1000 * 60 * 60 * 12
    private const val SIX_HOURS_IN_MS: Long = 1000 * 60 * 60 * 6
    private const val THREE_HOURS_IN_MS: Long = 1000 * 60 * 60 * 3
    private const val SERVICE_NAME_ABCLOGGER = "kaist.iclab.abclogger.collector.NotificationCollector"
    private const val SERVICE_NAME_ABCLOGGER2 = "kaist.iclab.abclogger.background.CollectorService"
    private const val SERVICE_NAME_MSBAND = "iclab.kaist.ac.kr.msband_logger.Service.AccessService"
    private const val SERVICE_NAME_POLAR = "fi.polar.beat.service.ExerciseService"
    private const val SERVICE_NAME_PACO = "com.pacoapp.paco.net.SyncService"
    private var msBandFlag = false
    private var polarFlag = false
    private var pacoFlag = false
    private var abcFlag = false



    private fun checkServiceRunning(context: Context) {
        msBandFlag = false  // 앱 accessibility 안켜면 실행 안됨;
        polarFlag = false   // 폴라앱 끄면 실행 안됨;
        pacoFlag = false    // 이거 그냥 무조건 실행되고 있음; 문제 있음;
        abcFlag = false     // not tested
        Log.d(TAG, "checkServiceRunning ()")
        val am = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        am.getRunningServices(Integer.MAX_VALUE).forEach {
            Log.i(TAG, it.service.className)
            when (it.service.className) {
                SERVICE_NAME_ABCLOGGER -> abcFlag = true
                SERVICE_NAME_ABCLOGGER2 -> abcFlag = true
                SERVICE_NAME_MSBAND -> msBandFlag = true
                SERVICE_NAME_PACO -> pacoFlag = true
                SERVICE_NAME_POLAR -> polarFlag = true
            }
        }
    }
}
