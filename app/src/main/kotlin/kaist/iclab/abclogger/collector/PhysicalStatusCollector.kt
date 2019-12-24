package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlarmManager
import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import kaist.iclab.abclogger.PhysicalStatusEntity
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.fillBaseInfo
import java.util.concurrent.TimeUnit

class PhysicalStatusCollector(val context: Context) : BaseCollector {
    private val alarmManager : AlarmManager by lazy {
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
                    .requestEmail()
                    .requestScopes(Scope(Scopes.FITNESS_ACTIVITY_READ), Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addExtension(fitnessOptions).build()

    class PhysicalStatusCollectorIntentService : IntentService(PhysicalStatusCollectorIntentService::class.java.name) {
        override fun onHandleIntent(intent: Intent?) {
            val account = GoogleSignIn.getLastSignedInAccount(this) ?: return

            val lastTime = SharedPrefs.lastPhysicalStatusAccessTime
            val currentTime = System.currentTimeMillis()

            if (lastTime < 0) {
                SharedPrefs.lastPhysicalStatusAccessTime = currentTime
                return
            }

            val request = DataReadRequest.Builder()
                    .setTimeRange(lastTime, currentTime, TimeUnit.MILLISECONDS)
                    .bucketByTime(15, TimeUnit.SECONDS)
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                    .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                    .build()

            val client = Fitness.getHistoryClient(this, account) ?: return
            val response = Tasks.await(client.readData(request)) ?: return

            response.buckets.mapNotNull { bucket ->
                bucket?.dataSets?.mapNotNull { dataSet ->
                    dataSet?.dataPoints?.mapNotNull { dataPoint ->
                        dataPoint.dataType.fields.forEach {
                            it.name
                        }
                        PhysicalStatusEntity(
                                type = dataPoint.dataType.name,
                                startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                                endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                                value = dataPoint.getValue(Field.FIELD_STEPS).asFloat()
                        ).fillBaseInfo(timestamp = dataPoint.getTimestamp(TimeUnit.MILLISECONDS))
                    }
                }?.flatten()
            }.flatten().run { putEntity(this) }
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedPhysicalStatus || !checkAvailability()) return

        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            val client = Fitness.getRecordingClient(context, account)

            Tasks.whenAll(dataTypes.map { dataType -> client.subscribe(dataType) })
        }?.addOnSuccessListener {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP)

        }?.addOnFailureListener {

        }
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedPhysicalStatus || !checkAvailability()) return


    }

    override fun checkAvailability(): Boolean =
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                GoogleSignIn.hasPermissions(account, fitnessOptions)
            } ?: false

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun newIntentForSetup(): Intent? = GoogleSignIn.getClient(context, signInOptions).signInIntent

}