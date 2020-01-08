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
import kaist.iclab.abclogger.Utils
import kaist.iclab.abclogger.LocationEntity
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.fillBaseInfo

class LocationCollector(val context: Context) : BaseCollector {
    private val client : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val receiver : BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_LOCATION_UPDATE || !LocationResult.hasResult(intent)) return

                LocationResult.extractResult(intent)?.lastLocation?.let { loc ->
                    LocationEntity(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracy = loc.accuracy,
                            speed = loc.speed
                    ).fillBaseInfo(timeMillis = loc.time)
                }?.run {
                    putEntity(this)
                }
            }
        }
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

    override fun onStart() {
        context.registerReceiver(receiver, filter)
        client.requestLocationUpdates(request, intent)
    }

    override fun onStop() {
        context.unregisterReceiver(receiver)
        client.removeLocationUpdates(intent)
    }

    override fun checkAvailability(): Boolean = Utils.checkPermissionAtRuntime(context, requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = null

    override val nameRes: Int?
        get() = R.string.data_name_location

    override val descriptionRes: Int?
        get() = R.string.data_desc_location

    companion object {
        private const val ACTION_LOCATION_UPDATE = "kaist.iclab.abclogger.ACTION_LOCATION_UPDATE"
        private const val REQUEST_CODE_LOCATION_UPDATE = 0xff

    }
}