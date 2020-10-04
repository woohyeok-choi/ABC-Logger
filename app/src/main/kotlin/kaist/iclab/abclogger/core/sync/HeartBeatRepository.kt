package kaist.iclab.abclogger.core.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.work.*
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.fitness.FitnessCollector
import kaist.iclab.abclogger.collector.embedded.EmbeddedSensorCollector
import kaist.iclab.abclogger.collector.external.PolarH10Collector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.transition.ActivityTransitionCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kaist.iclab.abclogger.commons.isPermissionGranted
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.commons.proto
import kaist.iclab.abclogger.core.AuthRepository
import kaist.iclab.abclogger.core.CollectorRepository
import kaist.iclab.abclogger.core.NotificationRepository
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.collector.Status
import kaist.iclab.abclogger.grpc.proto.DataTypeProtos
import kaist.iclab.abclogger.grpc.service.HeartBeatsOperationGrpcKt
import kaist.iclab.abclogger.grpc.proto.HeartBeatProtos
import kaist.iclab.abclogger.grpc.proto.SubjectProtos
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import java.util.concurrent.TimeUnit

class HeartBeatRepository(context: Context, params: WorkerParameters) :
    AbstractNetworkRepository<HeartBeatsOperationGrpcKt.HeartBeatsOperationCoroutineStub>(context, params),
    KoinComponent {
    private val collectorRepository: CollectorRepository by inject()

    override val stub: HeartBeatsOperationGrpcKt.HeartBeatsOperationCoroutineStub by lazy {
        HeartBeatsOperationGrpcKt.HeartBeatsOperationCoroutineStub(channel)
    }

    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(dispatcher) {
        try {
            if (AuthRepository.isSignedIn()) {
                updateRecord()
                checkPermission()
            }

            val dataStatusProto = collectorRepository.all.map { collector ->
                val type = collectorToDataType(collector)

                val description = collector.getDescription().associate { info ->
                    applicationContext.getString(info.stringRes) to info.value.toString()
                }

                proto(HeartBeatProtos.DataStatus.newBuilder()) {
                    name = collector.name
                    qualifiedName = collector.qualifiedName
                    dataType = type
                    turnedOnTime = collector.turnedOnTime
                    lastTimeWritten = collector.lastTimeDataWritten
                    recordsCollected = collector.recordsCollected
                    recordsUploaded = collector.recordsUploaded
                    status = when (collector.getStatus()) {
                        Status.On -> HeartBeatProtos.HeartBeat.Status.ON
                        Status.Off -> HeartBeatProtos.HeartBeat.Status.OFF
                        else -> HeartBeatProtos.HeartBeat.Status.ERROR
                    }
                    error = (collector.getStatus() as? Status.Error)?.message ?: ""
                    putAllOthers(description)
                }
            }

            val heartBeatProto = proto(HeartBeatProtos.HeartBeat.newBuilder()) {
                timestamp = System.currentTimeMillis()
                utcOffsetSec = TimeZone.getDefault().rawOffset / 1000
                subject = proto(SubjectProtos.Subject.newBuilder()) {
                    groupName = AuthRepository.groupName
                    email = AuthRepository.email()
                    instanceId = AuthRepository.instanceId()
                    source = AuthRepository.source
                    deviceManufacturer = AuthRepository.deviceManufacturer
                    deviceModel = AuthRepository.deviceModel
                    deviceVersion = AuthRepository.deviceVersion
                    deviceOs = AuthRepository.deviceOs
                    appId = AuthRepository.appId
                    appVersion = AuthRepository.appVersion
                }
                addAllCollector(dataStatusProto)
            }

            val deadlineStub = stub.withDeadlineAfter(1, TimeUnit.MINUTES)

            deadlineStub.createHeartBeat(heartBeatProto)
            Result.success()
        } catch (e: Exception) {
            Log.e(this@HeartBeatRepository.javaClass, e, report = true)
            Result.failure()
        } finally {
            shutdown()
        }
    }

    private fun updateRecord() {
        NotificationRepository.notifyForeground(
            applicationContext,
            collectorRepository.countTotalRecords(),
            collectorRepository.countUploadedRecords()
        )
    }

    private fun checkPermission() {
        val isOptimizationIgnored =
            CollectorRepository.isBatteryOptimizationIgnored(applicationContext)
        val isBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CollectorRepository.isBackgroundLocationAccessGranted(applicationContext)
        } else {
            true
        }
        val isPermissionGranted = isPermissionGranted(applicationContext, collectorRepository.permissions)

        if (!isOptimizationIgnored || !isBackgroundLocation || !isPermissionGranted) {
            NotificationRepository.notifyError(
                applicationContext,
                System.currentTimeMillis(),
                applicationContext.getString(R.string.ntf_error_text_precondition_error)
            )
        }
    }

    private fun collectorToDataType(collector: AbstractCollector<*>) = when (collector) {
        is ActivityTransitionCollector -> DataTypeProtos.DataType.PHYSICAL_ACTIVITY_TRANSITION
        is ActivityCollector -> DataTypeProtos.DataType.PHYSICAL_ACTIVITY
        is AppUsageCollector -> DataTypeProtos.DataType.APP_USAGE_EVENT
        is BatteryCollector -> DataTypeProtos.DataType.BATTERY
        is BluetoothCollector -> DataTypeProtos.DataType.BLUETOOTH
        is CallLogCollector -> DataTypeProtos.DataType.CALL_LOG
        is DeviceEventCollector -> DataTypeProtos.DataType.DEVICE_EVENT
        is EmbeddedSensorCollector -> DataTypeProtos.DataType.EMBEDDED_SENSOR
        is PolarH10Collector -> DataTypeProtos.DataType.EXTERNAL_SENSOR
        is InstalledAppCollector -> DataTypeProtos.DataType.INSTALLED_APP
        is KeyLogCollector -> DataTypeProtos.DataType.KEY_LOG
        is LocationCollector -> DataTypeProtos.DataType.LOCATION
        is MediaCollector -> DataTypeProtos.DataType.MEDIA
        is MessageCollector -> DataTypeProtos.DataType.MESSAGE
        is NotificationCollector -> DataTypeProtos.DataType.NOTIFICATION
        is FitnessCollector -> DataTypeProtos.DataType.FITNESS
        is SurveyCollector -> DataTypeProtos.DataType.SURVEY
        is DataTrafficCollector -> DataTypeProtos.DataType.DATA_TRAFFIC
        is WifiCollector -> DataTypeProtos.DataType.WIFI
        else -> DataTypeProtos.DataType.NOT_DATA_TYPE
    }

    companion object {
        private const val NAME_PERIODIC_WORKER =
            "${BuildConfig.APPLICATION_ID}.core.SyncRepository.HeartBeatWorker"
        private const val INTERVAL_MINUTE = 15L

        suspend fun start(context: Context) {
            val manager = WorkManager.getInstance(context)
            val isIdle = manager.getWorkInfosForUniqueWork(NAME_PERIODIC_WORKER).await()
                .find { it.state == WorkInfo.State.RUNNING } == null

            if (!isIdle) return
            val request = PeriodicWorkRequestBuilder<HeartBeatRepository>(INTERVAL_MINUTE, TimeUnit.MINUTES)
                .setInitialDelay(0, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            manager.enqueueUniquePeriodicWork(
                NAME_PERIODIC_WORKER,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun stop(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(NAME_PERIODIC_WORKER)
        }
    }
}