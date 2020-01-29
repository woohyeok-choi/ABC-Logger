package kaist.iclab.abclogger

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.*
import android.os.*
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import java.io.Serializable
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberExtensionProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

fun isNetworkAvailable(context: Context): Boolean =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null

fun isNonMeteredNetworkAvailable(context: Context): Boolean =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            (getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) ||
                    (getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)
        }

fun formatSameDayTimeYear(context: Context, then: Long, now: Long): String {
    val thenCal = GregorianCalendar.getInstance().apply {
        timeInMillis = then
    }
    val nowCal = GregorianCalendar.getInstance().apply {
        timeInMillis = now
    }

    val isSameDay = thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
            thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)

    val isSameYear = thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

    return when {
        isSameDay -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_TIME)
        isSameYear -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_DATE)
        else -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_YEAR)
    }
}

inline fun <reified T> checkServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val className = T::class.java.name
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) { // Deprecated 됐다지만 그냥 쓰라신다.
        if (className == service.service.className) {
            return true
        }
    }
    return false
}

fun View.setHorizontalPadding(pixelSize: Int) {
    val topPadding = paddingTop
    val bottomPadding = paddingBottom
    setPadding(pixelSize, topPadding, pixelSize, bottomPadding)
}

fun View.setVerticalPadding(pixelSize: Int) {
    val leftPadding = paddingLeft
    val rightPadding = paddingRight
    setPadding(leftPadding, paddingLeft, rightPadding, paddingRight)
}


fun Context.checkPermission(permissions: Collection<String>): Boolean =
        if (permissions.isEmpty()) {
            true
        } else {
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

fun Context.showSnackBar(view: View,
                         messageRes: Int,
                         showAlways: Boolean = false,
                         actionRes: Int? = null,
                         action: (() -> Unit)? = null
) {

    var snackBar = Snackbar.make(view, messageRes, if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }
    snackBar.show()
}

fun Fragment.showSnackBar(view: View,
                          messageRes: Int,
                          showAlways: Boolean = false,
                          actionRes: Int? = null,
                          action: (() -> Unit)? = null
) {
    var snackBar = Snackbar.make(view, messageRes, if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }
    snackBar.show()
}

fun Context.showToast(messageRes: Int, isShort: Boolean = true) =
        Toast.makeText(this, messageRes, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Context.showToast(message: String, isShort: Boolean = true) =
        Toast.makeText(this, message, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Fragment.showToast(messageRes: Int, isShort: Boolean = true) =
        Toast.makeText(requireContext(), messageRes, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Fragment.showToast(message: String, isShort: Boolean = true) =
        Toast.makeText(requireContext(), message, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Context.showToast(throwable: Throwable?, isShort: Boolean = true) {
    val msg = when (throwable) {
        is ABCException -> throwable.toString(this)
        else -> ABCException.wrap(throwable).toString(this)
    }
    Toast.makeText(this, msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Fragment.showToast(throwable: Throwable?, isShort: Boolean = true) {
    val msg = when (throwable) {
        is ABCException -> throwable.toString(requireContext())
        else -> ABCException.wrap(throwable).toString(requireContext())
    }
    Toast.makeText(requireContext(), msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

fun Context.isWhitelisted(): Boolean {
    val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return manager.isIgnoringBatteryOptimizations(packageName)
}

fun Intent.fillExtras(vararg params: Pair<String, Any?>) : Intent {
    params.forEach { (key, value) ->
        when (value) {
            null -> putExtra(key, null as Serializable?)
            is Int -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is CharSequence -> putExtra(key, value)
            is String -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Char -> putExtra(key, value)
            is Short -> putExtra(key, value)
            is Boolean -> putExtra(key, value)
            is Serializable -> putExtra(key, value)
            is Bundle -> putExtra(key, value)
            is Parcelable -> putExtra(key, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> putExtra(key, value)
                value.isArrayOf<String>() -> putExtra(key, value)
                value.isArrayOf<Parcelable>() -> putExtra(key, value)
            }
            is IntArray -> putExtra(key, value)
            is LongArray -> putExtra(key, value)
            is FloatArray -> putExtra(key, value)
            is DoubleArray -> putExtra(key, value)
            is CharArray -> putExtra(key, value)
            is ShortArray -> putExtra(key, value)
            is BooleanArray -> putExtra(key, value)
        }
    }
    return this
}

@Suppress("UNCHECKED_CAST")
fun Bundle.fillArguments(vararg params: Pair<String, Any?>) : Bundle {
    params.forEach { (key, value) ->
        when (value) {
            null -> putSerializable(key, null as Serializable?)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is CharSequence -> putCharSequence(key, value)
            is String -> putString(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            is Char -> putChar(key, value)
            is Short -> putShort(key, value)
            is Boolean -> putBoolean(key, value)
            is Serializable -> putSerializable(key, value)
            is Bundle -> putBundle(key, value)
            is Parcelable -> putParcelable(key, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> putCharSequenceArray(key, value as Array<CharSequence>)
                value.isArrayOf<String>() -> putStringArray(key, value as Array<String>)
                value.isArrayOf<Parcelable>() -> putParcelableArray(key, value as Array<Parcelable>)
            }
            is IntArray -> putIntArray(key, value)
            is LongArray -> putLongArray(key, value)
            is FloatArray -> putFloatArray(key, value)
            is DoubleArray -> putDoubleArray(key, value)
            is CharArray -> putCharArray(key, value)
            is ShortArray -> putShortArray(key, value)
            is BooleanArray -> putBooleanArray(key, value)
        }
    }
    return this
}

suspend fun httpGet(url: String, vararg params: Pair<String, Any?>): String? {
    val (_, _, result) = Fuel.get(url, params.toList()).awaitStringResponseResult()
    val (response, exception) = result
    if (exception != null) throw HttpRequestException(exception.message)

    return response
}

fun Context.safeRegisterReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = try {
    registerReceiver(receiver, filter)
} catch (e: IllegalArgumentException) {
}

fun Context.safeUnregisterReceiver(receiver: BroadcastReceiver) = try {
    unregisterReceiver(receiver)
} catch (e: IllegalArgumentException) {
}

suspend fun <T : Any> Single<T>.toCoroutine(context: CoroutineContext = EmptyCoroutineContext, throwable: Throwable? = null) = withContext(context) {
    suspendCoroutine<T> { continuation ->
        subscribe { result, exception ->
            if (exception != null) {
                continuation.resumeWithException(throwable ?: exception)
            } else {
                continuation.resume(result)
            }
        }
    }
}

suspend fun <T : Any> Task<T?>.toCoroutine(context: CoroutineContext = EmptyCoroutineContext, throwable: Throwable? = null) = withContext(context) {
    suspendCoroutine<T?> { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(throwable ?: task.exception ?: Exception())
            }
        }
    }
}

inline infix fun <reified T: Any> T?.merge(other: T): T {
    if(this == null) return other

    val nameToProperty = T::class.memberProperties.associateBy { it.name }
    val primaryConstructor = other::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associateWith { parameter ->
        val property = nameToProperty[parameter.name]!!

        return@associateWith property.get(other) ?: property.get(this)
    }
    return primaryConstructor.callBy(args)
}

inline fun <reified T : AccessibilityService> checkAccessibilityService(context: Context) : Boolean {
    val serviceName = "${context.packageName}/${T::class.java.name}"
    val isEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
    ) == 1
    val isIncluded = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )?.split(":")?.contains(serviceName) ?: false

    return isEnabled && isIncluded
}
