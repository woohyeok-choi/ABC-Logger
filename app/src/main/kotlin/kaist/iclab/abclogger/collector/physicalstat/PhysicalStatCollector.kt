package kaist.iclab.abclogger.collector.physicalstat

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
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PhysicalStatCollector(val context: Context) : BaseCollector {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessed: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

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
                GlobalScope.launch { handleUpdate() }
            }
        }
    }

    private suspend fun handleUpdate() {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        val curTime = System.currentTimeMillis()
        val lastTimeAccessed = Prefs.statusPhysicalStat?.lastTimeAccessed ?: curTime - TimeUnit.DAYS.toMillis(1)

        val request = DataReadRequest.Builder()
                .setTimeRange(lastTimeAccessed, curTime, TimeUnit.MILLISECONDS)
                .bucketByTime(30, TimeUnit.SECONDS)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .build()

        val client = Fitness.getHistoryClient(context, account) ?: return
        val response = client.readData(request).toCoroutine() ?: return

        response.buckets.mapNotNull { bucket ->
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
                                value = dataPoint.getValue(field).let {
                                    if (field == Field.FIELD_STEPS) it.asInt().toFloat() else it.asFloat()
                                }
                        ).fill(timeMillis = dataPoint.getTimestamp(TimeUnit.MILLISECONDS))
                    }
                }
            }?.flatten()
        }.flatten().also { entity ->
            ObjBox.put(entity)
            setStatus(Status(lastTime = curTime))
        }
        setStatus(Status(lastTimeAccessed = curTime))
    }

    override suspend fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw NoSignedGoogleAccountException()
        val client = Fitness.getRecordingClient(context, account)

        Tasks.whenAll(dataTypes.map { type -> client.subscribe(type) }).toCoroutine()

        val currentTime = System.currentTimeMillis()
        val threeHour: Long = TimeUnit.HOURS.toMillis(3)
        val lastTimeAccessed = (getStatus() as? Status)?.lastTimeAccessed ?: 0

        val triggerTime = if (lastTimeAccessed > 0 && lastTimeAccessed + threeHour >= currentTime) {
            lastTimeAccessed + threeHour
        } else {
            currentTime + TimeUnit.SECONDS.toMillis(10)
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

    override suspend fun checkAvailability(): Boolean {
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