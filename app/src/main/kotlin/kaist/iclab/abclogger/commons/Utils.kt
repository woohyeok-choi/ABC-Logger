package kaist.iclab.abclogger.commons

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.MutableLiveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.protobuf.GeneratedMessageLite
import kaist.iclab.abclogger.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.log10

fun isNetworkAvailable(context: Context) : Boolean = context.getSystemService<ConnectivityManager>()?.activeNetwork != null

fun isNonMeteredNetworkAvailable(context: Context): Boolean =
        context.getSystemService<ConnectivityManager>()?.let {
            it.getNetworkCapabilities(it.activeNetwork)
        }?.let {
            it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } == true

fun isPermissionGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

fun isPermissionGranted(context: Context, permissions: Collection<String>) : Boolean =
        if (permissions.isEmpty()) {
            true
        } else {
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

fun isPermissionGranted(context: Context, permissions: Array<String>) : Boolean =
        if (permissions.isEmpty()) {
            true
        } else {
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

inline fun <reified T: Service> isServiceRunning(context: Context) : Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val className = T::class.java.name
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) { // Deprecated 됐다지만 그냥 쓰라신다.
        if (className == service.service.className) {
            return true
        }
    }
    return false
}

inline fun <reified T : AccessibilityService> isAccessibilityServiceRunning(context: Context): Boolean {
    val packageName = context.packageName
    val contentResolver = context.contentResolver

    val serviceName = "${packageName}/${T::class.java.name}"
    val isEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
    ) == 1
    val isIncluded = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )?.split(":")?.contains(serviceName) ?: false

    return isEnabled && isIncluded
}

suspend fun getHttp(url: String, vararg params: Pair<String, Any?>): String? {
    val (_, _, result) = Fuel.get(url, params.toList()).awaitStringResponseResult()
    val (response, exception) = result
    if (exception != null) throw exception

    return response
}

internal fun getPreference(context: Context, name: String) =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

fun toHash(input: String, nCharNonHashed: Int, algorithm: String = "MD5"): String {
    if (input.isBlank()) return input
    val nCharNonHashedAdj = nCharNonHashed.coerceIn(0, input.length)

    val nonHashedString = input.take(nCharNonHashedAdj)
    val hashedString = input.takeLast(input.length - nCharNonHashedAdj).let {
        val bytes = MessageDigest.getInstance(algorithm).digest(it.toByteArray())
        bytes.joinToString("") { byte -> byte.toInt().and(0xFF).toString(16).padStart(2, '0') }
    }
    return "$nonHashedString\$$hashedString"
}

fun getApplicationName(packageManager: PackageManager, packageName: String?): String? {
    packageName ?: return null

    return try {
        packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
        ).let {
            packageManager.getApplicationLabel(it).toString()
        }
    } catch (e: Exception) {
        null
    }
}

fun isSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
        }
    } catch (e: Exception) {
        false
    }
}

fun isUpdatedSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
    } catch (e: Exception) {
        false
    }
}

fun isPowerSaveMode(context: Context) : Boolean = context.getSystemService<PowerManager>()?.isPowerSaveMode == true

fun <T> mutableLiveData(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): MutableLiveData<T> {
    val liveData: MutableLiveData<T> = MutableLiveData()
    val scope = CoroutineScope(context)
    scope.launch {
        liveData.postValue(block())
    }
    return liveData
}


fun crossFade(fadeIn: View, fadeOut: View) {
    fadeIn.apply {
        alpha = 0F
        visibility = View.VISIBLE
    }.animate()
            .alpha(1F)
            .setDuration(225)
            .setListener(null)
            .start()

    fadeOut
            .animate()
            .alpha(0F)
            .setDuration(225)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (fadeOut is ContentLoadingProgressBar) {
                        fadeOut.hide()
                    } else {
                        fadeOut.visibility = View.GONE
                    }
                }
            }).start()
}

inline fun <reified T: Enum<T>> safeEnumValueOf(value: String?, default: T? = null) : T {
    val def = default ?: enumValues<T>().first()
    if (value == null) return def
    return try {
        enumValueOf(value)
    } catch (e: Exception) {
        def
    }
}

fun <P : GeneratedMessageLite<P, B>, B : GeneratedMessageLite.Builder<P, B>> proto(
    builder: B,
    block: B.() -> Unit
): P {
    builder.block()

    return builder.build()
}

fun getColorFromAttr(context: Context, @AttrRes attrRes: Int) : Int? = TypedValue().apply {
    context.theme.resolveAttribute(attrRes, this, true)
}.let { value ->
    when {
        value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT -> value.data
        value.type == TypedValue.TYPE_REFERENCE -> try {
            ContextCompat.getColor(context, value.resourceId)
        } catch (e: Exception) {
            null
        }
        else -> null
    }
}

fun atLeastPositive(least: Long, value: Long) = if (value < 0) least else value