package kaist.iclab.abclogger.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.work.*
import com.google.firebase.iid.FirebaseInstanceId
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
import kaist.iclab.abclogger.collector.physicalstat.PhysicalStatCollector
import kaist.iclab.abclogger.collector.embedded.EmbeddedSensorCollector
import kaist.iclab.abclogger.collector.external.PolarH10Collector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.transition.ActivityTransitionCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kaist.iclab.abclogger.commons.Log
import kaist.iclab.abclogger.commons.proto
import kaist.iclab.abclogger.grpc.proto.CommonProtos
import kaist.iclab.abclogger.grpc.proto.HeartBeatProtos
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import java.util.concurrent.TimeUnit

class HeartBeatRepository(context: Context, params: WorkerParameters) :
    AbstractNetworkRepository(context, params),
    KoinComponent {
    private val collectorRepository: CollectorRepository by inject()

    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(dispatcher) {
        try {
            if (AuthRepository.isSignedIn()) {
                updateRecord()
                checkPermission()
            }

            val deadlineStub = stub.withDeadlineAfter(1, TimeUnit.MINUTES)
            val statusProto =
                collectorRepository.all.mapNotNull { collector -> createStatus(collector) }
            val heartBeatProto = proto(HeartBeatProtos.HeartBeat.newBuilder()) {
                timestamp = System.currentTimeMillis()
                utcOffsetSec = TimeZone.getDefault().rawOffset / 1000
                email = AuthRepository.email()
                deviceInfo = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
                deviceId = FirebaseInstanceId.getInstance().id
                addAllStatus(statusProto)
            }
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
        val isOptimizationIgnored = CollectorRepository.isBatteryOptimizationIgnored(applicationContext)
        val isBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CollectorRepository.isBackgroundLocationAccessGranted(applicationContext)
        } else {
            true
        }

        if (!isOptimizationIgnored || !isBackgroundLocation) {
            NotificationRepository.notifyError(
                applicationContext,
                System.currentTimeMillis(),
                null,
                applicationContext.getString(R.string.error_precondition_background_location_or_whitelist_denied)
            )
        }
    }

    private suspend fun createStatus(collector: AbstractCollector<*>): HeartBeatProtos.Status? {
        val type = when (collector) {
            is ActivityTransitionCollector -> CommonProtos.DataType.PHYSICAL_ACTIVITY_TRANSITION
            is ActivityCollector -> CommonProtos.DataType.PHYSICAL_ACTIVITY
            is AppUsageCollector -> CommonProtos.DataType.APP_USAGE_EVENT
            is BatteryCollector -> CommonProtos.DataType.BATTERY
            is BluetoothCollector -> CommonProtos.DataType.BLUETOOTH
            is CallLogCollector -> CommonProtos.DataType.CALL_LOG
            is DeviceEventCollector -> CommonProtos.DataType.DEVICE_EVENT
            is EmbeddedSensorCollector -> CommonProtos.DataType.EMBEDDED_SENSOR
            is PolarH10Collector -> CommonProtos.DataType.EXTERNAL_SENSOR
            is InstalledAppCollector -> CommonProtos.DataType.INSTALLED_APP
            is KeyLogCollector -> CommonProtos.DataType.KEY_LOG
            is LocationCollector -> CommonProtos.DataType.LOCATION
            is MediaCollector -> CommonProtos.DataType.MEDIA
            is MessageCollector -> CommonProtos.DataType.MESSAGE
            is NotificationCollector -> CommonProtos.DataType.NOTIFICATION
            is PhysicalStatCollector -> CommonProtos.DataType.PHYSICAL_STAT
            is SurveyCollector -> CommonProtos.DataType.SURVEY
            is DataTrafficCollector -> CommonProtos.DataType.DATA_TRAFFIC
            is WifiCollector -> CommonProtos.DataType.WIFI
            else -> null
        } ?: return null

        val count = collector.count()
        val status = collector.getStatus().associate { info ->
            applicationContext.getString(info.stringRes) to info.value.toString()
        }

        return proto(HeartBeatProtos.Status.newBuilder()) {
            dataType = type
            lastTimeWritten = collector.lastTimeDataWritten
            recordsCollected = collector.recordsCollected
            recordsUploaded = collector.recordsUploaded
            recordsRemained = count
            isEnabled = collector.isEnabled
            putAllStatus(status)
            errorMessage = collector.lastErrorMessage
        }
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