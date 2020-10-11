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
import kaist.iclab.abclogger.grpc.proto.DatumProtos
import kaist.iclab.abclogger.grpc.service.HeartBeatsOperationGrpcKt
import kaist.iclab.abclogger.grpc.proto.HeartBeatProtos
import kaist.iclab.abclogger.grpc.proto.SubjectProtos
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import java.util.concurrent.TimeUnit

class HeartBeatRepository(context: Context, params: WorkerParameters) :
    AbstractNetworkRepository(context, params),
    KoinComponent {
    private val collectorRepository: CollectorRepository by inject()

    private val stub: HeartBeatsOperationGrpcKt.HeartBeatsOperationCoroutineStub by lazy {
        HeartBeatsOperationGrpcKt.HeartBeatsOperationCoroutineStub(channel)
    }

    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(dispatcher) {
        try {
            if (AuthRepository.isSignedIn) {
                updateRecord()
                checkPermission()
            }

            val dataStatusProto = collectorRepository.all.map { collector ->
                val nRecords = collector.count()
                val description = collector.getDescription().associate { info ->
                    applicationContext.getString(info.stringRes) to info.value.toString()
                }

                proto(HeartBeatProtos.DataStatus.newBuilder()) {
                    name = collector.name
                    qualifiedName = collector.qualifiedName
                    datumType = collectorToDataType(collector)
                    turnedOnTime = collector.turnedOnTime
                    lastTimeWritten = collector.lastTimeDataWritten
                    recordsCollected = nRecords
                    recordsUploaded = collector.recordsUploaded
                    operation = when (collector.getStatus()) {
                        Status.On -> HeartBeatProtos.Operation.ON
                        Status.Off -> HeartBeatProtos.Operation.OFF
                        else -> HeartBeatProtos.Operation.ERROR
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
                    email = AuthRepository.email
                    instanceId = AuthRepository.instanceId
                    source = AuthRepository.source
                    deviceManufacturer = AuthRepository.deviceManufacturer
                    deviceModel = AuthRepository.deviceModel
                    deviceVersion = AuthRepository.deviceVersion
                    deviceOs = AuthRepository.deviceOs
                    appId = AuthRepository.appId
                    appVersion = AuthRepository.appVersion
                }
                addAllDataStatus(dataStatusProto)
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
            collectorRepository.nRecordsUploaded()
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
        is ActivityTransitionCollector -> DatumProtos.DatumType.PHYSICAL_ACTIVITY_TRANSITION
        is ActivityCollector -> DatumProtos.DatumType.PHYSICAL_ACTIVITY
        is AppUsageCollector -> DatumProtos.DatumType.APP_USAGE_EVENT
        is BatteryCollector -> DatumProtos.DatumType.BATTERY
        is BluetoothCollector -> DatumProtos.DatumType.BLUETOOTH
        is CallLogCollector -> DatumProtos.DatumType.CALL_LOG
        is DeviceEventCollector -> DatumProtos.DatumType.DEVICE_EVENT
        is EmbeddedSensorCollector -> DatumProtos.DatumType.EMBEDDED_SENSOR
        is PolarH10Collector -> DatumProtos.DatumType.EXTERNAL_SENSOR
        is InstalledAppCollector -> DatumProtos.DatumType.INSTALLED_APP
        is KeyLogCollector -> DatumProtos.DatumType.KEY_LOG
        is LocationCollector -> DatumProtos.DatumType.LOCATION
        is MediaCollector -> DatumProtos.DatumType.MEDIA
        is MessageCollector -> DatumProtos.DatumType.MESSAGE
        is NotificationCollector -> DatumProtos.DatumType.NOTIFICATION
        is FitnessCollector -> DatumProtos.DatumType.FITNESS
        is SurveyCollector -> DatumProtos.DatumType.SURVEY
        is DataTrafficCollector -> DatumProtos.DatumType.DATA_TRAFFIC
        is WifiCollector -> DatumProtos.DatumType.WIFI
        else -> DatumProtos.DatumType.UNRECOGNIZED
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
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun stop(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(NAME_PERIODIC_WORKER)
        }
    }
}