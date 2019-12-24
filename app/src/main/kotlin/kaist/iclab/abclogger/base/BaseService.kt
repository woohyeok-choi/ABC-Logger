package kaist.iclab.abclogger.base

import android.app.Service
import android.content.Intent
import android.util.Log

abstract class BaseService : Service() {
    protected val TAG: String = javaClass.simpleName

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }
}