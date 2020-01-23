package kaist.iclab.abclogger.collector.physicalstatus

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PhysicalStatusCollector(val context: Context) : BaseCollector {
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val dataTypes = arrayOf(
            DataType.TYPE_STEP_COUNT_DELTA,
            DataType.TYPE_STEP_COUNT_CUMULATIVE,
            DataType.TYPE_DISTANCE_DELTA,
            DataType.TYPE_DISTANCE_CUMULATIVE,
            DataType.TYPE_ACTIVITY_SEGMENT,
            DataType.TYPE_CALORIES_EXPENDED
    )

    private val fitnessOptions = FitnessOptions.builder().apply {
        dataTypes.forEach { dataType -> addDataType(dataType) }
    }.build()

    private val signInOptions: GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestScopes(Scope(Scopes.FITNESS_ACTIVITY_READ), Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addExtension(fitnessOptions).build()

    private val intent = PendingIntent.getBroadcast(context, REQUEST_CODE_PHYSICAL_STATUS_UPDATE, Intent(ACTION_UPDATE_PHYSICAL_STATUS), PendingIntent.FLAG_UPDATE_CURRENT)

    private val filter = IntentFilter().apply {
        addAction(ACTION_UPDATE_PHYSICAL_STATUS)
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_UPDATE_PHYSICAL_STATUS) return

                ObjBox.put(getPhysicalStatusEntities())
            }
        }
    }

    private fun getPhysicalStatusEntities(): List<PhysicalStatusEntity> {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return listOf()
        val lastTime = CollectorPrefs.lastAccessTimePhysicalStatus
        val currentTime = System.currentTimeMillis()

        if (lastTime < 0) return listOf()

        val request = DataReadRequest.Builder()
                .setTimeRange(lastTime, currentTime, TimeUnit.MILLISECONDS)
                .bucketByTime(30, TimeUnit.SECONDS)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .build()

        val client = Fitness.getHistoryClient(context, account) ?: return listOf()
        val response = Tasks.await(client.readData(request)) ?: return listOf()

        CollectorPrefs.lastAccessTimePhysicalStatus = currentTime

        return response.buckets.mapNotNull { bucket ->
            bucket?.dataSets?.mapNotNull { dataSet ->
                dataSet?.dataPoints?.mapNotNull { dataPoint ->
                    when (dataPoint.dataType) {
                        DataType.TYPE_STEP_COUNT_DELTA -> Field.FIELD_STEPS
                        DataType.TYPE_CALORIES_EXPENDED -> Field.FIELD_CALORIES
                        DataType.TYPE_DISTANCE_DELTA -> Field.FIELD_DISTANCE
                        else -> null
                    }?.let { field ->
                        PhysicalStatusEntity(
                                type = dataPoint.dataType.name,
                                startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                                endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                                value = dataPoint.getValue(field).asFloat()
                        ).fill(timeMillis = dataPoint.getTimestamp(TimeUnit.MILLISECONDS))
                    }
                }
            }?.flatten()
        }.flatten()
    }

    override suspend fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw NoSignedGoogleAccountException()
        val client = Fitness.getRecordingClient(context, account)

        suspendCoroutine<Void> { continuation ->
            Tasks.whenAll(
                    dataTypes.map { type -> client.subscribe(type) }
            ).addOnSuccessListener { result ->
                continuation.resume(result)
            }.addOnFailureListener { exception ->
                val exc = if (exception is ApiException) {
                    GoogleApiException(exception.statusCode)
                } else {
                    exception
                }
                continuation.resumeWithException(exc)
            }
        }

        val currentTime = System.currentTimeMillis()
        val threeHour: Long = TimeUnit.HOURS.toMillis(3)

        val triggerTime: Long = if (CollectorPrefs.lastAccessTimePhysicalStatus < 0 ||
                CollectorPrefs.lastAccessTimePhysicalStatus + threeHour < currentTime) {
            currentTime + 1000 * 5
        } else {
            CollectorPrefs.lastAccessTimePhysicalStatus + threeHour
        }

        context.safeRegisterReceiver(receiver, filter)
        alarmManager.cancel(intent)

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                threeHour,
                intent
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean {
        val isAccountAvailable = GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            GoogleSignIn.hasPermissions(account, fitnessOptions) &&
                    GoogleSignIn.hasPermissions(account,
                            Scope(Scopes.FITNESS_ACTIVITY_READ),
                            Scope(Scopes.FITNESS_LOCATION_READ_WRITE)
                    )
        } ?: false

        val isPermissionAvailable = context.checkPermission(requiredPermissions)

        return isAccountAvailable && isPermissionAvailable
    }

    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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

    override val newIntentForSetUp: Intent?
        get() = GoogleSignIn.getClient(context, signInOptions).signInIntent

    companion object {
        const val ACTION_UPDATE_PHYSICAL_STATUS = "${BuildConfig.APPLICATION_ID}.ACTION_UPDATE_PHYSICAL_STATUS"
        private const val REQUEST_CODE_PHYSICAL_STATUS_UPDATE = 0x03
    }
}