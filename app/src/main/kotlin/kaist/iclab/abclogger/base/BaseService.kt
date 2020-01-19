package kaist.iclab.abclogger.base

import android.app.Service
import android.content.Intent
import android.util.Log
import kaist.iclab.abclogger.AppLog

abstract class BaseService : Service() {
    protected val TAG: String = javaClass.simpleName

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.d(TAG, "onStartCommand()")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.d(TAG, "onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(TAG, "onDestroy()")
    }
}