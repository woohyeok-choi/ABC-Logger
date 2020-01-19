package kaist.iclab.abclogger.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kaist.iclab.abclogger.AppLog

abstract class BaseAppCompatActivity: AppCompatActivity() {
    protected val TAG: String = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(TAG, "onCreate()")
    }

    override fun onStart() {
        super.onStart()
        AppLog.d(TAG, "onStart()")
    }

    override fun onStop() {
        super.onStop()
        AppLog.d(TAG, "onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(TAG, "onDestroy()")
    }

    override fun onRestart() {
        super.onRestart()
        AppLog.d(TAG, "onRestart()")
    }

    override fun onResume() {
        super.onResume()
        AppLog.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        AppLog.d(TAG, "onPause()")
    }
}