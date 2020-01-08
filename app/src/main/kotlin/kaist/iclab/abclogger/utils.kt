package kaist.iclab.abclogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.*
import android.os.Build
import android.os.Environment
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.util.ArrayList
import kotlin.math.log10



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

    fun checkPermissionAtRuntime(context: Context, permissions: Collection<String>): Boolean =
            if (permissions.isEmpty()) {
                true
            } else {
                permissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
            }

    fun showSnackBar(view: View, messageRes: Int, showAlways: Boolean = false, actionRes: Int? = null, action: (() -> Unit)? = null) {
        var snackBar = Snackbar.make(view, messageRes, if(showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
        if(actionRes != null) {
            snackBar = snackBar.setAction(actionRes) {
                action?.invoke()
            }
        }
        snackBar.show()
    }

    fun showToast(context: Context?, messageRes: Int, isShort: Boolean = true) {
        Toast.makeText(context, messageRes, if(isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }

    fun showToast(context: Context?, message: String, isShort: Boolean = true) {
        Toast.makeText(context, message, if(isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }

    fun join(vararg texts: String?, separator: String = " ") = texts.filterNotNull().joinToString(separator = separator)

    fun fillButton(button: Button, isEnabled: Boolean, stringRes: Int? = null) {
        button.isEnabled = isEnabled
        button.setBackgroundColor(
                ContextCompat.getColor(button.context, if(isEnabled) R.color.color_button else R.color.color_disabled)
        )
        if(stringRes != null) {
            button.text = button.context.getString(stringRes)
        }
    }

    /**
     * TODO
     * will be moved to ObjBox control
     */
    fun isExternalStorageAvailable() : Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

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
