package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.LocationEntity
import kaist.iclab.abclogger.fillBaseInfo

class LocationCollector(val context: Context) : BaseCollector {
    private val client : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val intent : PendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_LOCATION_UPDATE, Intent(ACTION_LOCATION_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_LOCATION_UPDATE)
    }

    private val request = LocationRequest.create()
            .setInterval(1000 * 60 * 3)
            .setSmallestDisplacement(5.0F)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private val receiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_LOCATION_UPDATE || !LocationResult.hasResult(intent)) return

            LocationResult.extractResult(intent)?.lastLocation?.let { loc ->
                LocationEntity(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        speed = loc.speed
                ).fillBaseInfo(timestamp = loc.time)
            }?.run {
               putEntity(this)
            }
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedLocation || !checkAvailability()) return
        context.registerReceiver(receiver, filter)
        client.requestLocationUpdates(request, intent)
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedLocation || !checkAvailability()) return
        context.unregisterReceiver(receiver)
        client.removeLocationUpdates(intent)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun newIntentForSetup(): Intent? = null

    companion object {
        private const val ACTION_LOCATION_UPDATE = "kaist.iclab.abclogger.ACTION_LOCATION_UPDATE"
        private const val REQUEST_CODE_LOCATION_UPDATE = 0xff

    }
}