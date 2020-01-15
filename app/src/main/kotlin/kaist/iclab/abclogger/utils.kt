package kaist.iclab.abclogger

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.*
import android.os.*
import android.text.format.DateUtils
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
import com.google.android.material.snackbar.Snackbar
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import java.io.Serializable


object Utils {

    /**
     * TODO
     * Will be moved to sync mamanger
     */
    fun isNetworkAvailable(context: Context): Boolean =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null

    fun isNonMeteredNetworkAvailable(context: Context): Boolean =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
                (getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) ||
                        (getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)
            }

    fun formatTimeRange(context: Context, startInMillis: Long, endInMillis: Long): String {
        return DateUtils.formatDateRange(
                context, startInMillis, endInMillis, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NO_YEAR
        )
    }

    fun fillButton(button: Button, isEnabled: Boolean, stringRes: Int? = null) {
        button.isEnabled = isEnabled
        button.setBackgroundColor(
                ContextCompat.getColor(button.context, if (isEnabled) R.color.color_button else R.color.color_disabled)
        )
        if (stringRes != null) {
            button.text = button.context.getString(stringRes)
        }
    }

    fun showPermissionDialog(context: Context, permissions: Array<String>, granted: (() -> Unit)? = null, denied: (() -> Unit)? = null) {
        TedPermission.with(context)
                .setPermissions(*permissions)
                .setPermissionListener(object : PermissionListener {
                    override fun onPermissionGranted() {
                        granted?.invoke()
                    }

                    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                }).check()
    }

    @JvmStatic
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
}

inline fun <reified T> Context.checkServiceRunning(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

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

fun Context.checkServiceRunning(className: String): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    for (service in manager.getRunningServices(Integer.MAX_VALUE)) { // Deprecated 됐다지만 그냥 쓰라신다.
        if (className == service.service.className) {
            return true
        }
    }
    return false
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

fun Context.showToast(messageRes: Int, isShort: Boolean = true) {
    Toast.makeText(this, messageRes, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Context.showToast(message: String, isShort: Boolean = true) {
    Toast.makeText(this, message, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Fragment.showToast(messageRes: Int, isShort: Boolean) {
    Toast.makeText(requireContext(), messageRes, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Fragment.showToast(message: String, isShort: Boolean) {
    Toast.makeText(requireContext(), message, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

inline fun <T> Array<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <reified T : Activity> Context.startActivity(vararg params: Pair<String, Any?>, options: Bundle? = null) {
    val intent = Intent(this, T::class.java)
    fillIntentWithArguments(intent, params)
    startActivity(intent, options)
}

inline fun <reified T : Activity> Context.startService(vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    fillIntentWithArguments(intent, params)
    startService(intent)
}

inline fun <reified T : Activity> Context.startForegroundService(vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    fillIntentWithArguments(intent, params)
    startForegroundService(intent)
}

inline fun <reified T : Activity> Activity.startActivityForResult(requestCode: Int, options: Bundle? = null,
                                                                  vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    fillIntentWithArguments(intent, params)
    startActivityForResult(intent, requestCode, options)
}

inline fun <reified T : Activity> Fragment.startActivity(vararg params: Pair<String, Any?>, options: Bundle? = null) {
    val intent = Intent(requireContext(), T::class.java)
    fillIntentWithArguments(intent, params)
    startActivity(intent, options)
}

inline fun <reified T : Activity> Fragment.startActivity(options: Bundle? = null,
                                                         vararg params: Pair<String, Any?>) {
    val intent = Intent(requireContext(), T::class.java)
    fillIntentWithArguments(intent, params)
    startActivity(intent, options)
}

inline fun <reified T : Activity> Fragment.startActivityForResult(requestCode: Int, options: Bundle? = null,
                                                                  vararg params: Pair<String, Any?>) {
    val intent = Intent(requireContext(), T::class.java)
    fillIntentWithArguments(intent, params)
    startActivityForResult(intent, requestCode, options)
}

inline fun <reified T : Any> Context.intentFor(vararg params: Pair<String, Any?>): Intent {
    val intent = Intent(this, T::class.java)
    fillIntentWithArguments(intent, params)
    return intent
}

fun extraIntentFor(vararg params: Pair<String, Any?>): Intent {
    val intent = Intent()
    fillIntentWithArguments(intent, params)
    return intent
}

fun Context.checkWhitelist(): Boolean {
    val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return manager.isIgnoringBatteryOptimizations(packageName)
}

fun Context.sendBroadcast(action: String, vararg params: Pair<String, Any?>) {
    val intent = Intent(action)
    fillIntentWithArguments(intent, params)
    sendBroadcast(intent)
}

fun Fragment.fillArguments(vararg params: Pair<String, Any?>): Fragment {
    val bundle = Bundle()
    fillBundleWithArguments(bundle, params)
    arguments = Bundle()
    return this
}

fun fillIntentWithArguments(intent: Intent, params: Array<out Pair<String, Any?>>) =
        params.forEach { (key, value) ->
            when (value) {
                null -> intent.putExtra(key, null as Serializable?)
                is Int -> intent.putExtra(key, value)
                is Long -> intent.putExtra(key, value)
                is CharSequence -> intent.putExtra(key, value)
                is String -> intent.putExtra(key, value)
                is Float -> intent.putExtra(key, value)
                is Double -> intent.putExtra(key, value)
                is Char -> intent.putExtra(key, value)
                is Short -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
                is Serializable -> intent.putExtra(key, value)
                is Bundle -> intent.putExtra(key, value)
                is Parcelable -> intent.putExtra(key, value)
                is Array<*> -> when {
                    value.isArrayOf<CharSequence>() -> intent.putExtra(key, value)
                    value.isArrayOf<String>() -> intent.putExtra(key, value)
                    value.isArrayOf<Parcelable>() -> intent.putExtra(key, value)
                }
                is IntArray -> intent.putExtra(key, value)
                is LongArray -> intent.putExtra(key, value)
                is FloatArray -> intent.putExtra(key, value)
                is DoubleArray -> intent.putExtra(key, value)
                is CharArray -> intent.putExtra(key, value)
                is ShortArray -> intent.putExtra(key, value)
                is BooleanArray -> intent.putExtra(key, value)
            }
        }

@Suppress("UNCHECKED_CAST")
fun fillBundleWithArguments(bundle: Bundle, params: Array<out Pair<String, Any?>>) {
    params.forEach { (key, value) ->
        when (value) {
            null -> bundle.putSerializable(key, null as Serializable?)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is CharSequence -> bundle.putCharSequence(key, value)
            is String -> bundle.putString(key, value)
            is Float -> bundle.putFloat(key, value)
            is Double -> bundle.putDouble(key, value)
            is Char -> bundle.putChar(key, value)
            is Short -> bundle.putShort(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is Serializable -> bundle.putSerializable(key, value)
            is Bundle -> bundle.putBundle(key, value)
            is Parcelable -> bundle.putParcelable(key, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> bundle.putCharSequenceArray(key, value as Array<CharSequence>)
                value.isArrayOf<String>() -> bundle.putStringArray(key, value as Array<String>)
                value.isArrayOf<Parcelable>() -> bundle.putParcelableArray(key, value as Array<Parcelable>)
            }
            is IntArray -> bundle.putIntArray(key, value)
            is LongArray -> bundle.putLongArray(key, value)
            is FloatArray -> bundle.putFloatArray(key, value)
            is DoubleArray -> bundle.putDoubleArray(key, value)
            is CharArray -> bundle.putCharArray(key, value)
            is ShortArray -> bundle.putShortArray(key, value)
            is BooleanArray -> bundle.putBooleanArray(key, value)
        }
    }
}
