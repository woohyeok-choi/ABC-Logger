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
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class ActivityCollector(private val context: Context) : BaseCollector<ActivityCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null, override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_physical_activity)

    override val description: String = context.getString(R.string.data_desc_physical_activity)

    override val requiredPermissions: List<String> = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
            )
        }

    override val newIntentForSetUp: Intent? = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    override suspend fun onStart() {
        context.safeRegisterReceiver(receiver, filter)

        client.requestActivityUpdates(1000 * 15, activityIntent)
        client.requestActivityTransitionUpdates(transitionRequest, activityTransitionIntent)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        client.removeActivityUpdates(activityIntent)
        client.removeActivityTransitionUpdates(activityTransitionIntent)
    }

    override suspend fun checkAvailability(): Boolean {
        val isPermitted = context.checkPermission(requiredPermissions)
        val isLocationEnabled = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
        } else {
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled
        }
        return isPermitted && isLocationEnabled
    }

    private val client: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = when (intent?.action) {
                ACTION_ACTIVITY_UPDATE -> handleActivityRetrieval(intent)
                ACTION_ACTIVITY_TRANSITION_UPDATE -> handleActivityTransitionRetrieval(intent)
                else -> { }
            }
        }
    }

    private val activityIntent: PendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_ACTIVITY_UPDATE,
            Intent(ACTION_ACTIVITY_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val activityTransitionIntent: PendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE,
            Intent(ACTION_ACTIVITY_TRANSITION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val filter = IntentFilter().apply {
        addAction(ACTION_ACTIVITY_UPDATE)
        addAction(ACTION_ACTIVITY_TRANSITION_UPDATE)
    }

    private val transitionRequest = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.STILL
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

    private fun activityTypeToString(typeInt: Int) = when (typeInt) {
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
        DetectedActivity.ON_FOOT -> "ON_FOOT"
        DetectedActivity.RUNNING -> "RUNNING"
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.TILTING -> "TILTING"
        DetectedActivity.WALKING -> "WALKING"
        else -> "UNKNOWN"
    }

    private fun transitionEventToABCEvent(isEntered: Boolean, activityType: Int): String? {
        return if (isEntered) {
            when (activityType) {
                DetectedActivity.IN_VEHICLE -> AbcEvent.ACTIVITY_ENTER_IN_VEHICLE
                DetectedActivity.ON_BICYCLE -> AbcEvent.ACTIVITY_ENTER_ON_BICYCLE
                DetectedActivity.ON_FOOT -> AbcEvent.ACTIVITY_ENTER_ON_FOOT
                DetectedActivity.RUNNING -> AbcEvent.ACTIVITY_ENTER_RUNNING
                DetectedActivity.STILL -> AbcEvent.ACTIVITY_ENTER_STILL
                DetectedActivity.TILTING -> AbcEvent.ACTIVITY_ENTER_TILTING
                DetectedActivity.WALKING -> AbcEvent.ACTIVITY_ENTER_WALKING
                else -> null
            }
        } else {
            when (activityType) {
                DetectedActivity.IN_VEHICLE -> AbcEvent.ACTIVITY_EXIT_IN_VEHICLE
                DetectedActivity.ON_BICYCLE -> AbcEvent.ACTIVITY_EXIT_ON_BICYCLE
                DetectedActivity.ON_FOOT -> AbcEvent.ACTIVITY_EXIT_ON_FOOT
                DetectedActivity.RUNNING -> AbcEvent.ACTIVITY_EXIT_RUNNING
                DetectedActivity.STILL -> AbcEvent.ACTIVITY_EXIT_STILL
                DetectedActivity.TILTING -> AbcEvent.ACTIVITY_EXIT_TILTING
                DetectedActivity.WALKING -> AbcEvent.ACTIVITY_EXIT_WALKING
                else -> null
            }
        }
    }

    private fun handleActivityRetrieval(intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val curTime = System.currentTimeMillis()

        result.probableActivities?.map { detectedActivity ->
            PhysicalActivityEntity(
                    type = activityTypeToString(detectedActivity.type),
                    confidence = detectedActivity.confidence
            ).fill(timeMillis = result.time)
        }.also { entity ->
            launch {
                ObjBox.put(entity)
                setStatus(Status(lastTime = curTime))
            }
        }
    }

    private fun handleActivityTransitionRetrieval(intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return

        val curTime = System.currentTimeMillis()
        val result = ActivityTransitionResult.extractResult(intent)?.transitionEvents ?: return

        result.map { event ->
            val type = event.activityType
            val isEntered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER

            transitionEventToABCEvent(isEntered, type)?.let { AbcEvent.post(timestamp = curTime, eventType = it) }

            PhysicalActivityTransitionEntity(
                    type = activityTypeToString(type),
                    isEntered = isEntered
            ).fill(
                    timeMillis = curTime
            )
        }.also { entity ->
            launch {
                ObjBox.put(entity)
                setStatus(Status(lastTime = curTime))
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xf1
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0xf2

        private const val ACTION_ACTIVITY_TRANSITION_UPDATE = "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_TRANSITION_UPDATE"
        private const val ACTION_ACTIVITY_UPDATE = "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_UPDATE"
    }
}
