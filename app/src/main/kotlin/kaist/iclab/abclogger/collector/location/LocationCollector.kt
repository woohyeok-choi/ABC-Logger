package kaist.iclab.abclogger.collector.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.util.concurrent.TimeUnit

class LocationCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<LocationEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOfNotNull(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override val setupIntent: Intent? = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val intent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_LOCATION_UPDATE,
            Intent(ACTION_LOCATION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_LOCATION_UPDATE || !LocationResult.hasResult(intent)) return
            handleLocationRetrieval(intent)
        }
    }


    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf()

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_LOCATION_UPDATE)
        })

        val request = LocationRequest.create()
            .setInterval(TimeUnit.MINUTES.toMillis(3))
            .setSmallestDisplacement(5.0F)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        client.requestLocationUpdates(request, intent)

    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        client.removeLocationUpdates(intent)
    }

    override suspend fun count(): Long = dataRepository.count<LocationEntity>()

    override suspend fun flush(entities: Collection<LocationEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<LocationEntity> =
        dataRepository.find(0, limit)

    private fun handleLocationRetrieval(intent: Intent) = launch {
        val location = LocationResult.extractResult(intent)?.lastLocation ?: return@launch

        val entity = LocationEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            speed = location.speed
        )

        put(
            entity.apply {
                this.timestamp = location.time
            }
        )
    }

    companion object {
        private const val ACTION_LOCATION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_LOCATION_UPDATE"
        private const val REQUEST_CODE_LOCATION_UPDATE = 0xff
    }
}