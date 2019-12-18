package kaist.iclab.abclogger.common.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.common.InvalidContentException
import kaist.iclab.abclogger.common.NoNetworkAvailableException
import kaist.iclab.abclogger.common.NoSignedAccountException
import kaist.iclab.abclogger.common.NotVerifiedAccountException
import kaist.iclab.abclogger.data.FirestoreAccessor
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object Utils {
    fun utcOffsetInHour() : Float {
        return TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    }

    fun getApplicationName(context: Context, packageName: String) : String? {
        return try {
            val packageManager = context.packageManager
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)?.let {
                packageManager.getApplicationLabel(it)?.toString()
            }
        } catch (e: Exception) { null }
    }

    fun isSystemApp(context: Context, packageName: String) : Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)?.let {
                it.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
            }
        } catch (e: Exception) { false } ?: false
    }

    fun isUpdatedSystemApp(context: Context, packageName: String) : Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)?.let {
                it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            }
        } catch (e: Exception) { false } ?: false
    }
}