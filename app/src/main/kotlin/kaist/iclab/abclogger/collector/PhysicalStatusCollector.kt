package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlarmManager
import android.app.IntentService
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
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.base.BaseCollector
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

            val lastTime = SharedPrefs.lastAccessTimePhysicalStatus
            val currentTime = System.currentTimeMillis()

            if (lastTime < 0) {
                SharedPrefs.lastAccessTimePhysicalStatus = currentTime
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
                        ).fillBaseInfo(timeMillis = dataPoint.getTimestamp(TimeUnit.MILLISECONDS))
                    }
                }?.flatten()
            }.flatten().run { putEntity(this) }
        }
    }

    override fun onStart() {
        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            val client = Fitness.getRecordingClient(context, account)
            Tasks.whenAll(dataTypes.map { dataType -> client.subscribe(dataType) })
        }?.addOnSuccessListener {

        }?.addOnFailureListener {

        }
    }

    override fun onStop() {

    }

    override fun checkAvailability(): Boolean {
        val isAccountAvailable = GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            GoogleSignIn.hasPermissions(account, fitnessOptions)
        } ?: false

        val isPermissionAvailable = Utils.checkPermissionAtRuntime(context, requiredPermissions)

        return isAccountAvailable && isPermissionAvailable
    }

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PhysicalStatusSettingActivity::class.java)

    class PhysicalStatusSettingActivity : BaseAppCompatActivity() {

    }
}