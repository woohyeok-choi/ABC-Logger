package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import com.google.android.gms.location.*
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.common.util.PermissionUtils

class ActivityCollector(val context: Context) : BaseCollector {
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

    private val client: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
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
    }.flatten().let { ActivityTransitionRequest(it)}


    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ACTIVITY_UPDATE &&
                    ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult.extractResult(intent)?.let { result ->
                    result.probableActivities?.map { detectedActivity ->
                        PhysicalActivityEntity(
                                type = activityTypeToString(detectedActivity.type),
                                confidence = detectedActivity.confidence
                        ).fillBaseInfo(timestamp = result.time)
                    }
                }.run { putEntity(this) }

            } else if (intent?.action == ACTION_ACTIVITY_TRANSITION_UPDATE &&
                    ActivityTransitionResult.hasResult(intent)) {
                val elapsedTimeNano = SystemClock.elapsedRealtimeNanos()
                val timeMillis = System.currentTimeMillis()

                ActivityTransitionResult.extractResult(intent)?.let { result ->
                    result.transitionEvents?.map { event ->
                        PhysicalActivityTransitionEntity(
                                type = activityTypeToString(event.activityType),
                                isEntered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
                        ).fillBaseInfo(
                                timestamp = timeMillis + (event.elapsedRealTimeNanos - elapsedTimeNano)
                        )
                    }
                }.run { putEntity(this) }
            }
        }
    }


    override fun start() {
        if(!SharedPrefs.isProvidedActivity || !checkAvailability()) return
        context.registerReceiver(receiver, filter)

        client.requestActivityUpdates(1000 * 15, activityIntent)
        client.requestActivityTransitionUpdates(transitionRequest, activityTransitionIntent)

    }

    override fun stop() {
        if(!SharedPrefs.isProvidedActivity || !checkAvailability()) return

        context.unregisterReceiver(receiver)

        client.removeActivityUpdates(activityIntent)
        client.removeActivityTransitionUpdates(activityTransitionIntent)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
    )

    override fun newIntentForSetup(): Intent? = null

    companion object {
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xf1
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0xf2

        private const val ACTION_ACTIVITY_TRANSITION_UPDATE = "kaist.iclab.abclogger.ACTION_ACTIVITY_TRANSITION_UPDATE"
        private const val ACTION_ACTIVITY_UPDATE = "kaist.iclab.abclogger.ACTION_ACTIVITY_UPDATE"
    }
}