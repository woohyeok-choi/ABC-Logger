package kaist.iclab.abclogger.background.collector

import android.Manifest
import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.NoSignedGoogleAccountException
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.PreferenceAccessor
import kaist.iclab.abclogger.data.entities.PhysicalStatusEntity
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class GoogleFitnessCollector(val context: Context) : BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFuture?.isDone == false) return
        status.postValue(Status.STARTED)

        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                status.postValue(Status.RUNNING)

                val pref = PreferenceAccessor.getInstance(context)
                val hour = TimeUnit.HOURS.toMillis(1)
                val now = System.currentTimeMillis()

                val to = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = now
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val from = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = if(pref.lastTimeGoogleFitnessAccessed < 0) to - hour * 2 else pref.lastTimeGoogleFitnessAccessed
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                Log.d(TAG, "$from ~ $to")
                for(i in from until to step hour) {
                    if(i + hour < to) {
                        collect(uuid, group, email, i, i + hour)
                        pref.lastTimeGoogleFitnessAccessed = i + hour
                    }
                }
            } catch (e: Exception) {
                when(e) {
                    is SecurityException, is NoSignedGoogleAccountException -> {
                        stopCollection()
                        status.postValue(Status.ABORTED(e))
                    }
                }
            }
        }, 0, 15, TimeUnit.MINUTES)
    }

    override fun stopCollection() {
        scheduledFuture?.cancel(true)
        status.postValue(Status.CANCELED)
    }

    private fun collect(uuid: String, group: String, email: String, from: Long, to: Long) {

        if(!checkEnableToCollect(context)) throw SecurityException("Not permitted to Google Fitness ExperimentData.")
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: throw NoSignedGoogleAccountException()

        val box = App.boxFor<PhysicalStatusEntity>()

        val request = DataReadRequest.Builder()
            .setTimeRange(from, to, TimeUnit.MILLISECONDS)
            .bucketByActivitySegment(15, TimeUnit.SECONDS)
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
            .build()

        Fitness.getHistoryClient(context, account).readData(request)
            .addOnSuccessListener { response ->
                readRawStepCounts(response.buckets, uuid, group, email).let { entities ->
                    val totalEntity = PhysicalStatusEntity(
                        type = "TotalStepCounts",
                        value = deriveTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    val totalActivityEntity = PhysicalStatusEntity(
                        type = "TotalActivityStepCounts",
                        value = deriveActivityTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    box.put(entities)
                    box.put(totalEntity)
                    box.put(totalActivityEntity)

                    entities.forEach {
                        Log.d(TAG, "Box.put(" +
                            "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                            "experimentGroup = ${it.experimentGroup}, entity = $it)")
                    }

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalEntity.timestamp}, subjectEmail = ${totalEntity.subjectEmail}, experimentUuid = ${totalEntity.experimentUuid}, " +
                        "experimentGroup = ${totalEntity.experimentGroup}, entity = $totalEntity)")

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalActivityEntity.timestamp}, subjectEmail = ${totalActivityEntity.subjectEmail}, experimentUuid = ${totalActivityEntity.experimentUuid}, " +
                        "experimentGroup = ${totalActivityEntity.experimentGroup}, entity = $totalActivityEntity)")
                }

                readRawCalories(response.buckets, uuid, group, email).let { entities ->
                    val totalEntity = PhysicalStatusEntity(
                        type = "TotalCalories",
                        value = deriveTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    val totalActivityEntity = PhysicalStatusEntity(
                        type = "TotalActivityCalories",
                        value = deriveActivityTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    box.put(entities)
                    box.put(totalEntity)
                    box.put(totalActivityEntity)

                    entities.forEach {
                        Log.d(TAG, "Box.put(" +
                            "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                            "experimentGroup = ${it.experimentGroup}, entity = $it)")
                    }

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalEntity.timestamp}, subjectEmail = ${totalEntity.subjectEmail}, experimentUuid = ${totalEntity.experimentUuid}, " +
                        "experimentGroup = ${totalEntity.experimentGroup}, entity = $totalEntity)")

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalActivityEntity.timestamp}, subjectEmail = ${totalActivityEntity.subjectEmail}, experimentUuid = ${totalActivityEntity.experimentUuid}, " +
                        "experimentGroup = ${totalActivityEntity.experimentGroup}, entity = $totalActivityEntity)")
                }

                readRawDistance(response.buckets, uuid, group, email).let { entities ->
                    val totalEntity = PhysicalStatusEntity(
                        type = "TotalDistance",
                        value = deriveTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    val totalActivityEntity = PhysicalStatusEntity(
                        type = "TotalActivityDistance",
                        value = deriveActivityTotalValue(entities),
                        startTime = from,
                        endTime = to
                    ).apply {
                        timestamp = to
                        utcOffset = Utils.utcOffsetInHour()
                        experimentUuid = uuid
                        experimentGroup = group
                        subjectEmail = email
                        isUploaded = false
                    }
                    box.put(entities)
                    box.put(totalEntity)
                    box.put(totalActivityEntity)

                    entities.forEach {
                        Log.d(TAG, "Box.put(" +
                            "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                            "experimentGroup = ${it.experimentGroup}, entity = $it)")
                    }

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalEntity.timestamp}, subjectEmail = ${totalEntity.subjectEmail}, experimentUuid = ${totalEntity.experimentUuid}, " +
                        "experimentGroup = ${totalEntity.experimentGroup}, entity = $totalEntity)")

                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${totalActivityEntity.timestamp}, subjectEmail = ${totalActivityEntity.subjectEmail}, experimentUuid = ${totalActivityEntity.experimentUuid}, " +
                        "experimentGroup = ${totalActivityEntity.experimentGroup}, entity = $totalActivityEntity)")
                }
            }

    }

    private fun checkPhysicalActivity(activity: String) : Boolean {
        return activity != FitnessActivities.STILL &&
            activity != FitnessActivities.UNKNOWN &&
            activity != FitnessActivities.IN_VEHICLE &&
            activity != FitnessActivities.TILTING &&
            activity != FitnessActivities.ELEVATOR &&
            activity != FitnessActivities.ESCALATOR &&
            activity != FitnessActivities.SLEEP &&
            activity != FitnessActivities.SLEEP_AWAKE &&
            activity != FitnessActivities.SLEEP_DEEP &&
            activity != FitnessActivities.SLEEP_LIGHT &&
            activity != FitnessActivities.SLEEP_REM
    }

    private fun readRawStepCounts(buckets: List<Bucket>, uuid: String, group: String, email: String) : List<PhysicalStatusEntity> {
        return buckets.mapNotNull { bucket ->
            bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)?.dataPoints?.map {
                PhysicalStatusEntity(
                    activity = bucket.activity,
                    type = "RawStepCounts",
                    value = it.getValue(Field.FIELD_STEPS).asInt().toFloat(),
                    startTime = it.getStartTime(TimeUnit.MILLISECONDS),
                    endTime = it.getEndTime(TimeUnit.MILLISECONDS)
                ).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }.flatten()
    }

    private fun readRawCalories(buckets: List<Bucket>, uuid: String, group: String, email: String) : List<PhysicalStatusEntity> {
        return buckets.asSequence().mapNotNull { bucket ->
            bucket.getDataSet(DataType.TYPE_CALORIES_EXPENDED)?.dataPoints?.map {
                PhysicalStatusEntity(
                    activity = bucket.activity,
                    type = "RawCaloriesExpended",
                    value = it.getValue(Field.FIELD_CALORIES).asFloat(),
                    startTime = it.getStartTime(TimeUnit.MILLISECONDS),
                    endTime = it.getEndTime(TimeUnit.MILLISECONDS)
                ).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }.flatten().toList()
    }

    private fun readRawDistance(buckets: List<Bucket>, uuid: String, group: String, email: String) : List<PhysicalStatusEntity> {
        return buckets.asSequence().mapNotNull { bucket ->
            bucket.getDataSet(DataType.TYPE_DISTANCE_DELTA)?.dataPoints?.map {
                PhysicalStatusEntity(
                    activity = bucket.activity,
                    type = "RawDistance",
                    value = it.getValue(Field.FIELD_DISTANCE).asFloat(),
                    startTime = it.getStartTime(TimeUnit.MILLISECONDS),
                    endTime = it.getEndTime(TimeUnit.MILLISECONDS)
                ).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }.flatten().toList()
    }

    private fun deriveActivityTotalValue(entities: List<PhysicalStatusEntity>) : Float {
        return entities.fold(0F) { acc, physicalStatusEntity ->
            return@fold if(checkPhysicalActivity(physicalStatusEntity.activity)) {
                acc + physicalStatusEntity.value
            } else {
                acc
            }
        }
    }

    private fun deriveTotalValue(entities: List<PhysicalStatusEntity>) : Float {
        return entities.fold(0F) { acc, physicalStatusEntity -> acc + physicalStatusEntity.value }
    }

    companion object {
        private val TAG : String = GoogleFitnessCollector::class.java.simpleName

        private val DATA_TYPES = listOf(
            DataType.TYPE_STEP_COUNT_DELTA,
            DataType.TYPE_ACTIVITY_SEGMENT,
            DataType.TYPE_CALORIES_EXPENDED,
            DataType.TYPE_DISTANCE_DELTA
        )

        private val FITNESS_OPTIONS = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED)
            .addDataType(DataType.TYPE_DISTANCE_DELTA)
            .build()

        fun checkEnableToCollect(context: Context) = GoogleSignIn.getLastSignedInAccount(context)?.let { GoogleSignIn.hasPermissions(it, FITNESS_OPTIONS) } ?: false

        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun newIntentForSetup(context: Context) = GoogleSignIn.getClient(context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .addExtension(FITNESS_OPTIONS)
                .build()
        ).let { client -> client.signOut().onSuccessTask { Tasks.call { client.signInIntent } } }

        fun subscribeFitnessData(context: Context) : Task<Void> {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: throw NoSignedGoogleAccountException()
            return Fitness.getRecordingClient(context, account).let { client ->
                Tasks.whenAll(DATA_TYPES.map { dataType -> client.subscribe(dataType) })
            }
        }

        fun unsubscribeFitnessData(context: Context) : Task<Void>? {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: throw NoSignedGoogleAccountException()

            return Fitness.getRecordingClient(context, account).let { client ->
                Tasks.whenAll(DATA_TYPES.map { dataType -> client.unsubscribe(dataType) })
            }
        }

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }
    }
}