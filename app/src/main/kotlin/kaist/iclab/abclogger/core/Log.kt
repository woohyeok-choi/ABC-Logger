package kaist.iclab.abclogger.core

import android.util.Log as AndroidLog
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.reflect.KClass

object Log {
    fun d(kClass: KClass<*>? = null, any: Any?) = d(kClass?.qualifiedName, any)

    fun d(clazz: Class<*>? = null, any: Any?) = d(clazz?.name, any)

    fun d(tag: String? = null, any: Any?) = AndroidLog.d(tag ?: "AppLog", any?.toString() ?: "")

    fun e(kClass: KClass<*>? = null, throwable: Throwable? = null, message: String? = null, report: Boolean = false) =
            e(kClass?.qualifiedName, throwable, message, report)

    fun e(clazz: Class<*>? = null, throwable: Throwable? = null, message: String? = null, report: Boolean = false) =
            e(clazz?.name, throwable, message, report)

    fun e(tag: String? = null, throwable: Throwable? = null, message: String? = null, report: Boolean = false) {
        AndroidLog.e(tag ?: "AppLog", arrayOf(throwable?.message, message).filter { !it.isNullOrBlank() }.joinToString(separator = ": "))
        throwable?.printStackTrace()
        throwable?.let { if (report) FirebaseCrashlytics.getInstance().recordException(it) }
    }

    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun sendReports() {
        FirebaseCrashlytics.getInstance().sendUnsentReports()
    }
}