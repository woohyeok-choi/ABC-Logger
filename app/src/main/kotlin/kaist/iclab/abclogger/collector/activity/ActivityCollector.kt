package kaist.iclab.abclogger.collector.activity

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import com.google.android.gms.location.*
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.collector.stringifyActivityType
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import java.util.concurrent.TimeUnit

class ActivityCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<PhysicalActivityEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOfNotNull(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null
    )

    override val setupIntent: Intent? = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    private val client: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ACTIVITY_UPDATE -> handleActivityRetrieval(intent)
            }
        }
    }

    private val activityIntent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_ACTIVITY_UPDATE,
            Intent(ACTION_ACTIVITY_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun getDescription(): Array<Description> = arrayOf()

    override fun isAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            ) != Settings.Secure.LOCATION_MODE_OFF
        } else {
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled
        }
    }

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_ACTIVITY_UPDATE)
        })

        client.requestActivityUpdates(TimeUnit.SECONDS.toMillis(15), activityIntent)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        client.removeActivityUpdates(activityIntent)
    }

    override suspend fun count(): Long = dataRepository.count<PhysicalActivityEntity>()

    override suspend fun flush(entities: Collection<PhysicalActivityEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<PhysicalActivityEntity> =
        dataRepository.find(0, limit)

    private fun handleActivityRetrieval(intent: Intent) = launch {
        if (!ActivityRecognitionResult.hasResult(intent)) return@launch

        val result = ActivityRecognitionResult.extractResult(intent)?.probableActivities
            ?: return@launch
        if (result.isEmpty()) return@launch

        val curTime = System.currentTimeMillis()
        val activities = result.map { detectedActivity ->
            PhysicalActivityEntity.Activity(
                type = stringifyActivityType(detectedActivity.type),
                confidence = detectedActivity.confidence
            )
        }

        put(
            PhysicalActivityEntity(activities = activities).apply {
                timestamp = curTime
            }
        )
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0xf2
        private const val ACTION_ACTIVITY_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_UPDATE"
    }


}
