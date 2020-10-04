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
import androidx.navigation.ActivityNavigatorDestinationBuilder
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.activity
import androidx.navigation.fragment.fragment
import com.google.android.gms.location.*
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.AbstractCollector
import kaist.iclab.abclogger.commons.stringifyActivityType
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.NotificationRepository
import java.util.concurrent.TimeUnit

class ActivityCollector(
    context: Context,
    name: String,
    qualifiedName: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOfNotNull(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null
    )

    override val setupIntent: Intent? = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    private val client: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ACTIVITY_UPDATE -> handleActivityRetrieval(intent)
                ACTION_ACTIVITY_TRANSITION_UPDATE -> handleActivityTransitionRetrieval(intent)
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

    private val activityTransitionIntent by lazy {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE,
            Intent(ACTION_ACTIVITY_TRANSITION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun getAdditionalInformation(): Array<Info> = arrayOf()

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
        val request = listOf(
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

        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_ACTIVITY_UPDATE)
            addAction(ACTION_ACTIVITY_TRANSITION_UPDATE)
        })

        client.requestActivityUpdates(TimeUnit.SECONDS.toMillis(15), activityIntent)
        client.requestActivityTransitionUpdates(request, activityTransitionIntent)
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        client.removeActivityUpdates(activityIntent)
        client.removeActivityTransitionUpdates(activityTransitionIntent)
    }

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

        write(PhysicalActivityEntity(activities = activities), curTime)
    }

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
            )
        }

        write(entities, timestamp)
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xf1
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0xf2
        private const val ACTION_ACTIVITY_TRANSITION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_TRANSITION_UPDATE"
        private const val ACTION_ACTIVITY_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_UPDATE"
    }
}
