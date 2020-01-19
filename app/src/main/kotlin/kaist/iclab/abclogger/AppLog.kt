package kaist.iclab.abclogger

import android.util.Log
import com.crashlytics.android.Crashlytics

object AppLog {
    fun d(tag: String? = null, any: Any?) = Log.d(tag ?: "AppLog", any?.toString() ?: "")
    fun e(tag: String? = null, throwable: Throwable?, message: String? = null, showStackTrace: Boolean = true) {
        Log.e(tag ?: "AppLog", arrayOf(throwable?.message, message).filter { !it.isNullOrBlank() }.joinToString(separator = ": "))
        if (showStackTrace) throwable?.printStackTrace()
    }
    fun dd(tag: String? = null, any: Any?) = Crashlytics.log(arrayOf(tag, any?.toString()).filter { !it.isNullOrBlank() }.joinToString(": "))
    fun ee(throwable: Throwable) = Crashlytics.logException(throwable)
}