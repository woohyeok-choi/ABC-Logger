package kaist.iclab.abclogger.common.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {
    fun isNetworkAvailable(context: Context) : Boolean {
        val activeNetworkInfo = (context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager).activeNetworkInfo

        return activeNetworkInfo?.isConnected ?: false
    }

    fun isWifiAvailable(context: Context) : Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            manager.activeNetworkInfo?.isConnected ?: false && manager.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        } else {
            manager.activeNetwork?.let {
                manager.getNetworkCapabilities(it).hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } ?: false
        }
    }
}