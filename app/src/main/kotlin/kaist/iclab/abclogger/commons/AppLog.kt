package kaist.iclab.abclogger.commons

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AppLog {
    fun d(tag: String? = null, any: Any?) = Log.d(tag ?: "AppLog", any?.toString() ?: "")
    fun e(tag: String? = null, throwable: Throwable?, message: String? = null, showStackTrace: Boolean = true) {
        Log.e(tag
                ?: "AppLog", arrayOf(throwable?.message, message).filter { !it.isNullOrBlank() }.joinToString(separator = ": "))
        if (showStackTrace) throwable?.printStackTrace()
    }

    fun ee(throwable: Throwable) {
        throwable.printStackTrace()
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}