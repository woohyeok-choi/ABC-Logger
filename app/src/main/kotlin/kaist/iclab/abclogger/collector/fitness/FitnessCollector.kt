package kaist.iclab.abclogger.collector.fitness

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
import com.google.android.gms.fitness.*

import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.formatDateTime
import kaist.iclab.abclogger.collector.stringifyFitnessDeviceType
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.*
import java.util.concurrent.TimeUnit

class FitnessCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<FitnessEntity>(
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

    override val setupIntent: Intent? by lazy {
        val option = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .addExtension(fitnessOptions)
            .build()

        GoogleSignIn.getClient(context, option).signInIntent
    }

    data class DataTypeSpec(
        val dataType: DataType,
        val aggregatedDataType: DataType,
        val fields: Array<Field>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DataTypeSpec

            if (dataType != other.dataType) return false
            if (aggregatedDataType != other.aggregatedDataType) return false
            if (!fields.contentEquals(other.fields)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = dataType.hashCode()
            result = 31 * result + aggregatedDataType.hashCode()
            result = 31 * result + fields.contentHashCode()
            return result
        }
    }

    private val dataSpecStepCount = DataTypeSpec(
        dataType = DataType.TYPE_STEP_COUNT_DELTA,
        aggregatedDataType = DataType.AGGREGATE_STEP_COUNT_DELTA,
        fields = arrayOf(Field.FIELD_STEPS)
    )

    private val dataSpecDistance = DataTypeSpec(
        dataType = DataType.TYPE_DISTANCE_DELTA,
        aggregatedDataType = DataType.AGGREGATE_DISTANCE_DELTA,
        fields = arrayOf(Field.FIELD_DISTANCE)
    )

    private val dataSpecActivity = DataTypeSpec(
        dataType = DataType.TYPE_ACTIVITY_SEGMENT,
        aggregatedDataType = DataType.AGGREGATE_ACTIVITY_SUMMARY,
        fields = arrayOf(Field.FIELD_ACTIVITY, Field.FIELD_DURATION, Field.FIELD_NUM_SEGMENTS)
    )

    private val dataSpecCalories = DataTypeSpec(
        dataType = DataType.TYPE_CALORIES_EXPENDED,
        aggregatedDataType = DataType.AGGREGATE_CALORIES_EXPENDED,
        fields = arrayOf(Field.FIELD_CALORIES)
    )

    private val dataSpecs =
        listOf(dataSpecStepCount, dataSpecDistance, dataSpecActivity, dataSpecCalories)

    private val fitnessOptions = FitnessOptions.builder().apply {
        dataSpecs.forEach {
            addDataType(it.dataType, FitnessOptions.ACCESS_READ)
            addDataType(it.dataType, FitnessOptions.ACCESS_WRITE)
        }
    }.build()

    private var lastTimeStepCountWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeDistanceWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeCaloriesWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeActivityWritten by ReadWriteStatusLong(Long.MIN_VALUE)

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PHYSICAL_STATUS_UPDATE,
            Intent(ACTION_UPDATE_PHYSICAL_STATUS),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_UPDATE_PHYSICAL_STATUS) return
            handlePhysicalStatRetrieval()
        }
    }


    override fun isAvailable(): Boolean = try {
        GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(context),
            fitnessOptions
        )
    } catch (e: Exception) {
        false
    }

    override fun getDescription(): Array<Description> = arrayOf(
        R.string.collector_fitness_info_step_count_written with
                formatDateTime(context, lastTimeStepCountWritten),
        R.string.collector_fitness_info_stat_activity_written with
                formatDateTime(context, lastTimeActivityWritten),
        R.string.collector_fitness_info_stat_distance_written with
                formatDateTime(context, lastTimeDistanceWritten),
        R.string.collector_fitness_info_stat_calories_written with
                formatDateTime(context, lastTimeCaloriesWritten)
    )

    override suspend fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw GoogleApiError.noSignedAccount()
        val recordingClient = Fitness.getRecordingClient(context, account)

        Tasks.whenAll(
            dataSpecs.map { recordingClient.subscribe(it.dataType) }
        ).toCoroutine()

        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_UPDATE_PHYSICAL_STATUS)
        })

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20),
            TimeUnit.HOURS.toMillis(1),
            intent
        )
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<FitnessEntity>()

    override suspend fun flush(entities: Collection<FitnessEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<FitnessEntity> =
        dataRepository.find(0, limit)

    private fun handlePhysicalStatRetrieval() = launch {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw GoogleApiError.noSignedAccount()
        val historyClient = Fitness.getHistoryClient(context, account)
        val toTime = System.currentTimeMillis()

        val fromTimeStepCount = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeStepCountWritten
        )
        val fromTimeActivity = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeActivityWritten
        )
        val fromTimeDistance = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeDistanceWritten
        )
        val fromTimeCalories = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeCaloriesWritten
        )

        /**
         * Extract step counts
         */
        val stepCounts = extractData(
            historyClient = historyClient,
            fromTime = fromTimeStepCount,
            toTime = toTime,
            spec = dataSpecStepCount
        ).filter { entity ->
            entity.endTime >= fromTimeStepCount
        }

        stepCounts.forEach { put(it) }

        lastTimeStepCountWritten =
            stepCounts.maxOfOrNull { it.endTime }?.coerceAtLeast(lastTimeStepCountWritten)
                ?: lastTimeStepCountWritten

        /**
         * Extract activity segments
         */
        val activities = extractData(
            historyClient = historyClient,
            fromTime = fromTimeActivity,
            toTime = toTime,
            spec = dataSpecActivity
        ).filter { entity ->
            entity.endTime >= fromTimeActivity
        }

        activities.forEach { put(it) }

        lastTimeActivityWritten =
            activities.maxOfOrNull { it.endTime }?.coerceAtLeast(lastTimeActivityWritten)
                ?: lastTimeActivityWritten

        /**
         * Extract distances
         */
        val distances = extractData(
            historyClient = historyClient,
            fromTime = fromTimeDistance,
            toTime = toTime,
            spec = dataSpecDistance
        ).filter { entity ->
            entity.endTime >= fromTimeDistance
        }

        distances.forEach { put(it) }

        lastTimeDistanceWritten =
            distances.maxOfOrNull { it.endTime }?.coerceAtLeast(lastTimeDistanceWritten)
                ?: lastTimeDistanceWritten


        /**
         * Extract calories
         */
        val calories = extractData(
            historyClient = historyClient,
            fromTime = fromTimeCalories,
            toTime = toTime,
            spec = dataSpecCalories
        ).sortedBy { entity ->
            entity.endTime
        }.filter { entity ->
            entity.endTime >= fromTimeCalories
        }

        calories.forEach { put(it) }

        lastTimeCaloriesWritten =
            calories.maxOfOrNull { it.endTime }?.coerceAtLeast(lastTimeCaloriesWritten)
                ?: lastTimeCaloriesWritten


    }

    private suspend fun extractData(
        historyClient: HistoryClient,
        fromTime: Long,
        toTime: Long,
        spec: DataTypeSpec
    ): List<FitnessEntity> {
        val request = DataReadRequest.Builder()
            .setTimeRange(fromTime, toTime, TimeUnit.MILLISECONDS)
            .bucketByTime(30, TimeUnit.SECONDS)
            .aggregate(spec.dataType)
            .build()
        val response = historyClient.readData(request).toCoroutine() ?: return listOf()

        return response.buckets?.mapNotNull { bucket ->
            bucket.dataSets?.mapNotNull { dataSet ->
                dataSet.dataPoints?.mapNotNull { dataPoint ->
                    extractData(dataPoint, spec)
                }
            }
        }?.flatten()?.flatten() ?: listOf()
    }

    private fun extractData(dataPoint: DataPoint?, spec: DataTypeSpec): FitnessEntity? {
        dataPoint ?: return null
        val type = dataPoint.dataType?.name ?: return null
        if (spec.dataType.name != type && spec.aggregatedDataType.name != type) return null

        val startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
        val endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS)
        val values = spec.fields.mapNotNull { field ->
            val value = dataPoint.getValue(field) ?: return@mapNotNull null
            if (field.name == Field.FIELD_ACTIVITY.name) {
                value.asActivity()
            } else {
                when (field.format) {
                    Field.FORMAT_FLOAT -> value.asFloat().toString()
                    Field.FORMAT_INT32 -> value.asInt().toString()
                    Field.FORMAT_STRING -> value.asString().toString()
                    else -> null
                }
            }
        }.joinToString("; ")
        if (values.isBlank()) return null

        val dataSourceName = dataPoint.dataSource?.streamName ?: ""
        val dataSourcePackageName = dataPoint.dataSource?.appPackageName ?: ""
        val fitnessDeviceModel = dataPoint.dataSource.device?.model ?: ""
        val fitnessDeviceManufacturer = dataPoint.dataSource.device?.manufacturer ?: ""
        val fitnessDeviceType = stringifyFitnessDeviceType(dataPoint.dataSource.device?.type)

        return FitnessEntity(
            type = spec.dataType.name,
            startTime = startTime,
            endTime = endTime,
            value = values,
            fitnessDeviceModel = fitnessDeviceModel,
            fitnessDeviceManufacturer = fitnessDeviceManufacturer,
            fitnessDeviceType = fitnessDeviceType,
            dataSourceName = dataSourceName,
            dataSourcePackageName = dataSourcePackageName
        ).apply {
            this.timestamp = endTime
        }
    }

    companion object {
        private const val ACTION_UPDATE_PHYSICAL_STATUS =
            "${BuildConfig.APPLICATION_ID}.ACTION_UPDATE_PHYSICAL_STATUS"
        private const val REQUEST_CODE_PHYSICAL_STATUS_UPDATE = 0x03
    }
}