package kaist.iclab.abclogger.commons

import android.util.Log as AndroidLog
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.reflect.KClass

object Log {
    fun d(kClass: KClass<*>? = null, any: Any?) = d(kClass?.qualifiedName, any)

    fun d(clazz: Class<*>? = null, any: Any?) = d(clazz?.name, any)

    fun d(tag: String? = null, any: Any?) = AndroidLog.d(tag ?: "AppLog", any?.toString() ?: "")

    fun e(kClass: KClass<*>? = null, throwable: Throwable? = null, message: String? = null, showStackTrace: Boolean = true, report: Boolean = false) =
            e(kClass?.qualifiedName, throwable, message, showStackTrace, report)

    fun e(clazz: Class<*>? = null, throwable: Throwable? = null, message: String? = null, showStackTrace: Boolean = true, report: Boolean = false) =
            e(clazz?.name, throwable, message, showStackTrace, report)

    fun e(tag: String? = null, throwable: Throwable? = null, message: String? = null, showStackTrace: Boolean = true, report: Boolean = false) {
        AndroidLog.e(tag ?: "AppLog", arrayOf(throwable?.message, message).filter { !it.isNullOrBlank() }.joinToString(separator = ": "))

        throwable?.run {
            if (showStackTrace) printStackTrace()
            if (report) FirebaseCrashlytics.getInstance().recordException(this)
        }
    }

    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun sendReports() {
        FirebaseCrashlytics.getInstance().sendUnsentReports()
    }
}