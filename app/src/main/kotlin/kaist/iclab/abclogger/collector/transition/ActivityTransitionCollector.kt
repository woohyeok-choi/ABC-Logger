package kaist.iclab.abclogger.collector.transition

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

class ActivityTransitionCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<PhysicalActivityTransitionEntity>(
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
                ACTION_ACTIVITY_TRANSITION_UPDATE -> handleActivityTransitionRetrieval(intent)
            }
        }
    }

    private val activityTransitionIntent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE,
            Intent(ACTION_ACTIVITY_TRANSITION_UPDATE),
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
        /** [20.11.19]
         * Activity transition takes only 5 types of DetectedActivity:
         * IN_VEHICLE, ON_BICYCLE, RUNNING, STILL, WALKING.
         *
         * https://developer.android.com/guide/topics/location/transitions
         */

        val request = listOf(
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.RUNNING,
                DetectedActivity.STILL,
                DetectedActivity.WALKING
        ).map { activity ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }.flatten().let { ActivityTransitionRequest(it) }

        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_ACTIVITY_TRANSITION_UPDATE)
        })

        client.requestActivityTransitionUpdates(request, activityTransitionIntent)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        client.removeActivityTransitionUpdates(activityTransitionIntent)
    }

    override suspend fun count(): Long = dataRepository.count<PhysicalActivityTransitionEntity>()

    override suspend fun flush(entities: Collection<PhysicalActivityTransitionEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<PhysicalActivityTransitionEntity> = dataRepository.find(0, limit)

    private fun handleActivityTransitionRetrieval(intent: Intent) = launch {
        if (!ActivityTransitionResult.hasResult(intent)) return@launch

        val result = ActivityTransitionResult.extractResult(intent)?.transitionEvents
            ?: return@launch
        if (result.isEmpty()) return@launch

        val timestamp = System.currentTimeMillis()
        val entities = result.map { event ->
            PhysicalActivityTransitionEntity(
                type = stringifyActivityType(event.activityType),
                isEntered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ).apply {
                this.timestamp = timestamp
            }
        }
        entities.forEach { put(it) }
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xf1
        private const val ACTION_ACTIVITY_TRANSITION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_TRANSITION_UPDATE"
    }
}
