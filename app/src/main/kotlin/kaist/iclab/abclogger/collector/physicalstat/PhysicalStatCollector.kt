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
import kaist.iclab.abclogger.commons.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class PhysicalStatCollector(private val context: Context) : BaseCollector<PhysicalStatCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessed: Long? = null,
                      val lastTimeAccessedStepCount: Long? = null,
                      val lastTimeAccessedDistance: Long? = null,
                      val lastTimeAccessedCalories: Long? = null) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_physical_stat)

    override val description: String = context.getString(R.string.data_desc_physical_stat)

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

    override val newIntentForSetUp: Intent = GoogleSignIn.getClient(context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestScopes(Scope(Scopes.FITNESS_ACTIVITY_READ), Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addExtension(FITNESS_OPTION).build()
    ).signInIntent

    override suspend fun checkAvailability(): Boolean {
        val isAccountAvailable = try {
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                GoogleSignIn.hasPermissions(account, FITNESS_OPTION) &&
                        GoogleSignIn.hasPermissions(account,
                                Scope(Scopes.FITNESS_ACTIVITY_READ),
                                Scope(Scopes.FITNESS_LOCATION_READ_WRITE)
                        )
            }
        } catch (e: Exception) {
            null
        } ?: false

        val isPermissionAvailable = context.checkPermission(requiredPermissions)

        return isAccountAvailable && isPermissionAvailable
    }

    override suspend fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw NoSignedGoogleAccountException()
        val client = Fitness.getRecordingClient(context, account)

        Tasks.whenAll(DATA_TYPES.map { type -> client.subscribe(type) }).toCoroutine()

        val currentTime = System.currentTimeMillis()
        val threeHour: Long = TimeUnit.HOURS.toMillis(3)
        val lastTimeAccessed = getStatus()?.lastTimeAccessed ?: 0

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

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val intent = PendingIntent.getBroadcast(context, REQUEST_CODE_PHYSICAL_STATUS_UPDATE, Intent(ACTION_UPDATE_PHYSICAL_STATUS), PendingIntent.FLAG_UPDATE_CURRENT)

    private val filter = IntentFilter().apply {
        addAction(ACTION_UPDATE_PHYSICAL_STATUS)
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_UPDATE_PHYSICAL_STATUS) return
                handlePhysicalStatRetrieval()
            }
        }
    }

    private suspend fun extractData(curTime: Long,
                                    lastTimeAccessed: Long?,
                                    dataType: DataType,
                                    aggregateDataType: DataType,
                                    field: Field): List<PhysicalStatEntity> {
        val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw NoSignedGoogleAccountException()
        val client = Fitness.getHistoryClient(context, account)
                ?: throw NoSignedGoogleAccountException()

        val lastTime = lastTimeAccessed ?: curTime - TimeUnit.DAYS.toMillis(1)
        val request = DataReadRequest.Builder()
                .setTimeRange(lastTime, curTime, TimeUnit.MILLISECONDS)
                .bucketByTime(30, TimeUnit.SECONDS)
                .aggregate(dataType, aggregateDataType)
                .build()

        val response = client.readData(request).toCoroutine() ?: return listOf()

        return response.buckets.mapNotNull { bucket ->
            bucket?.dataSets?.mapNotNull { dataSet ->
                dataSet?.dataPoints?.mapNotNull { dataPoint ->
                    if (dataPoint.dataType != dataType) {
                        null
                    } else {
                        PhysicalStatEntity(
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
        }.flatten().filter { entity ->
            entity.endTime > lastTime
        }
    }

    private fun handlePhysicalStatRetrieval() = launch {
        try {
            val curTime = System.currentTimeMillis()
            val steps = extractData(
                    curTime = curTime,
                    lastTimeAccessed = getStatus()?.lastTimeAccessedStepCount,
                    dataType = DataType.TYPE_STEP_COUNT_DELTA,
                    aggregateDataType = DataType.AGGREGATE_STEP_COUNT_DELTA,
                    field = Field.FIELD_STEPS
            )
            val distance = extractData(
                    curTime = curTime,
                    lastTimeAccessed = getStatus()?.lastTimeAccessedDistance,
                    dataType = DataType.TYPE_DISTANCE_DELTA,
                    aggregateDataType = DataType.AGGREGATE_DISTANCE_DELTA,
                    field = Field.FIELD_DISTANCE
            )
            val calories = extractData(
                    curTime = curTime,
                    lastTimeAccessed = getStatus()?.lastTimeAccessedCalories,
                    dataType = DataType.TYPE_CALORIES_EXPENDED,
                    aggregateDataType = DataType.AGGREGATE_CALORIES_EXPENDED,
                    field = Field.FIELD_CALORIES
            )

            ObjBox.put(steps)
            ObjBox.put(distance)
            ObjBox.put(calories)

            setStatus(
                    Status(
                            lastTimeAccessed = curTime,
                            lastTime = curTime,
                            lastTimeAccessedStepCount = steps.maxBy { it.endTime }?.endTime,
                            lastTimeAccessedDistance = distance.maxBy { it.endTime }?.endTime,
                            lastTimeAccessedCalories = calories.maxBy { it.endTime }?.endTime
                    )
            )
        } catch (e: Exception) {
            notifyError(e)
        }
    }

    companion object {
        const val ACTION_UPDATE_PHYSICAL_STATUS = "${BuildConfig.APPLICATION_ID}.ACTION_UPDATE_PHYSICAL_STATUS"

        private const val REQUEST_CODE_PHYSICAL_STATUS_UPDATE = 0x03

        private val DATA_TYPES = arrayOf(
                DataType.TYPE_STEP_COUNT_DELTA,
                DataType.TYPE_STEP_COUNT_CUMULATIVE,
                DataType.TYPE_DISTANCE_DELTA,
                DataType.TYPE_DISTANCE_CUMULATIVE,
                DataType.TYPE_ACTIVITY_SEGMENT,
                DataType.TYPE_CALORIES_EXPENDED
        )

        private val FITNESS_OPTION = FitnessOptions.builder().apply {
            DATA_TYPES.forEach { dataType -> addDataType(dataType) }
        }.build()
    }
}