package kaist.iclab.abclogger

import android.app.ActivityManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import kaist.iclab.abclogger.collector.Base
import kaist.iclab.abclogger.collector.activity.PhysicalActivityEntity
import kaist.iclab.abclogger.collector.activity.PhysicalActivityTransitionEntity
import kaist.iclab.abclogger.collector.appusage.AppUsageEventEntity
import kaist.iclab.abclogger.collector.battery.BatteryEntity
import kaist.iclab.abclogger.collector.bluetooth.BluetoothEntity
import kaist.iclab.abclogger.collector.call.CallLogEntity
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.externalsensor.ExternalSensorEntity
import kaist.iclab.abclogger.collector.install.InstalledAppEntity
import kaist.iclab.abclogger.collector.internalsensor.SensorEntity
import kaist.iclab.abclogger.collector.keylog.KeyLogEntity
import kaist.iclab.abclogger.collector.location.LocationEntity
import kaist.iclab.abclogger.collector.media.MediaEntity
import kaist.iclab.abclogger.collector.message.MessageEntity
import kaist.iclab.abclogger.collector.notification.NotificationEntity
import kaist.iclab.abclogger.collector.physicalstat.PhysicalStatEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.traffic.DataTrafficEntity
import kaist.iclab.abclogger.collector.wifi.WifiEntity
import kaist.iclab.abclogger.commons.Notifications
import kaist.iclab.abclogger.grpc.DataOperationsCoroutineGrpc
import kaist.iclab.abclogger.grpc.DataOperationsGrpc
import kaist.iclab.abclogger.grpc.DatumProto
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class SyncWorker2(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val limiter: Semaphore = Semaphore(1)

    private val cancelIntent: PendingIntent = PendingIntent.getService(
            applicationContext, REQUEST_CODE_CANCEL_SYNC, Intent(ACTION_CANCEL_SYNC), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val foregroundInfo = ForegroundInfo(
            Notifications.ID_SYNC_PROGRESS,
            Notifications.build(
                    context = applicationContext,
                    channelId = Notifications.CHANNEL_ID_PROGRESS,
                    title = applicationContext.getString(R.string.ntf_title_sync),
                    text = applicationContext.getString(R.string.ntf_text_sync),
                    progress = 0,
                    indeterminate = true,
                    intent = cancelIntent,
                    actions = listOf(
                            NotificationCompat.Action.Builder(
                                    R.drawable.baseline_close_white_24,
                                    applicationContext.getString(R.string.ntf_action_sync_cancel),
                                    WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
                            ).build()
                    )
            )
    )

    override fun doWork(): Result {
        setForegroundAsync(foregroundInfo)

        val channel: ManagedChannel = AndroidChannelBuilder
                .forTarget(if (BuildConfig.IS_TEST_MODE) BuildConfig.TEST_SERVER_ADDRESS else BuildConfig.SERVER_ADDRESS)
                .usePlaintext()
                .context(applicationContext)
                .executor(Dispatchers.IO.asExecutor())
                .build()

        val stub = DataOperationsGrpc.newFutureStub(channel)

        uploadAll(stub)

        Prefs.lastTimeDataSync = System.currentTimeMillis()

        try {
            channel.shutdownNow()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }


    private inline fun <reified T : Base> upload(stub: DataOperationsGrpc.DataOperationsFutureStub) {
        val ids = ObjBox.query<T>()?.build()?.findIds() ?: return
        val size = ids.size

        (0 until size step N_UPLOADS).forEach { offset ->
           val deadlineStub = stub.withDeadlineAfter(1, TimeUnit.MINUTES)
            (offset..offset + N_UPLOADS).map { index ->
                    try {
                        val id = ids[index]
                        val entity = ObjBox.get<T>(id) ?: throw Exception("No corresponding entity.")
                        val proto = toProto(entity) ?: throw Exception("No corresponding protobuf.")
                        deadlineStub.createDatum(proto).add
                        ObjBox.remove(entity)
                    } catch (e: Exception) {
                    }

            }
        }
    }

    private fun isLowMemory(): Boolean {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val maxHeapSize = manager.largeMemoryClass
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedPercentage = usedMemory.toFloat() / (maxHeapSize * 1e6).toFloat()
        Log.d("SyncWorker", "usedPercentage: $usedPercentage / maxHeapSize: $maxHeapSize")

        return usedPercentage > 0.5F
    }

    private suspend fun uploadAll(stub: DataOperationsCoroutineGrpc.DataOperationsCoroutineStub) {
        upload<PhysicalActivityTransitionEntity>(stub)
        upload<PhysicalActivityEntity>(stub)
        upload<AppUsageEventEntity>(stub)
        upload<BatteryEntity>(stub)
        upload<BluetoothEntity>(stub)
        upload<CallLogEntity>(stub)
        upload<DeviceEventEntity>(stub)
        upload<ExternalSensorEntity>(stub)
        upload<InstalledAppEntity>(stub)
        upload<KeyLogEntity>(stub)
        upload<LocationEntity>(stub)
        upload<MediaEntity>(stub)
        upload<MessageEntity>(stub)
        upload<NotificationEntity>(stub)
        upload<PhysicalStatEntity>(stub)
        upload<SensorEntity>(stub)
        upload<SurveyEntity>(stub)
        upload<DataTrafficEntity>(stub)
        upload<WifiEntity>(stub)
    }

    private fun <T> toProto(entity: T): DatumProto.Datum? {
        if (entity !is Base) return null

        val builder = DatumProto.Datum.newBuilder()
                .setTimestamp(entity.timestamp)
                .setUtcOffset(entity.utcOffset)
                .setSubjectEmail(entity.subjectEmail)
                .setDeviceInfo(entity.deviceInfo)

        return when (entity) {
            is PhysicalActivityTransitionEntity -> builder.setPhysicalActivityTransition(
                    DatumProto.Datum.PhysicalActivityTransition.newBuilder()
                            .setType(entity.type)
                            .setIsEntered(entity.isEntered)
                            .build()
            )
            is PhysicalActivityEntity -> builder.setPhysicalActivity(
                    DatumProto.Datum.PhysicalActivity.newBuilder()
                            .setType(entity.type)
                            .setConfidence(entity.confidence)
                            .build()
            )
            is AppUsageEventEntity -> builder.setAppUsageEvent(
                    DatumProto.Datum.AppUsageEvent.newBuilder()
                            .setName(entity.name)
                            .setPackageName(entity.packageName)
                            .setType(entity.type)
                            .setIsSystemApp(entity.isSystemApp)
                            .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                            .build()
            )
            is BatteryEntity -> builder.setBattery(
                    DatumProto.Datum.Battery.newBuilder()
                            .setLevel(entity.level)
                            .setScale(entity.scale)
                            .setTemperature(entity.temperature)
                            .setVoltage(entity.voltage)
                            .setHealth(entity.health)
                            .setPluggedType(entity.pluggedType)
                            .setStatus(entity.status)
                            .build()
            )
            is BluetoothEntity -> builder.setBluetooth(
                    DatumProto.Datum.Bluetooth.newBuilder()
                            .setDeviceName(entity.deviceName)
                            .setAddress(entity.address)
                            .setRssi(entity.rssi)
                            .build()
            )
            is CallLogEntity -> builder.setCallLog(
                    DatumProto.Datum.CallLog.newBuilder()
                            .setDuration(entity.duration)
                            .setNumber(entity.number)
                            .setType(entity.type)
                            .setPresentation(entity.presentation)
                            .setDataUsage(entity.dataUsage)
                            .setContactType(entity.contactType)
                            .setIsStarred(entity.isStarred)
                            .setIsPinned(entity.isPinned)
                            .build()
            )
            is DeviceEventEntity -> builder.setDeviceEvent(
                    DatumProto.Datum.DeviceEvent.newBuilder()
                            .setType(entity.type)
                            .build()
            )
            is ExternalSensorEntity -> builder.setExternalSensor(
                    DatumProto.Datum.ExternalSensor.newBuilder()
                            .setSensorId(entity.sensorId)
                            .setName(entity.name)
                            .setDescription(entity.description)
                            .setFirstValue(entity.firstValue)
                            .setSecondValue(entity.secondValue)
                            .setThirdValue(entity.thirdValue)
                            .setFourthValue(entity.fourthValue)
                            .setCollection(entity.collection)
                            .build()
            )
            is InstalledAppEntity -> builder.setInstalledApp(
                    DatumProto.Datum.InstalledApp.newBuilder()
                            .setName(entity.name)
                            .setPackageName(entity.packageName)
                            .setIsSystemApp(entity.isSystemApp)
                            .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                            .setFirstInstallTime(entity.firstInstallTime)
                            .setLastUpdateTime(entity.lastUpdateTime)
                            .build()
            )
            is SensorEntity -> builder.setInternalSensor(
                    DatumProto.Datum.InternalSensor.newBuilder()
                            .setType(entity.type)
                            .setAccuracy(entity.accuracy)
                            .setFirstValue(entity.firstValue)
                            .setSecondValue(entity.secondValue)
                            .setThirdValue(entity.thirdValue)
                            .setFourthValue(entity.fourthValue)
                            .build()
            )
            is KeyLogEntity -> builder.setKeyLog(
                    DatumProto.Datum.KeyLog.newBuilder()
                            .setName(entity.name)
                            .setPackageName(entity.packageName)
                            .setIsSystemApp(entity.isSystemApp)
                            .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                            .setDistance(entity.distance)
                            .setTimeTaken(entity.timeTaken)
                            .setKeyboardType(entity.keyboardType)
                            .setPrevKeyType(entity.prevKeyType)
                            .setCurrentKeyType(entity.currentKeyType)
                            .build()
            )
            is LocationEntity -> builder.setLocation(
                    DatumProto.Datum.Location.newBuilder()
                            .setLatitude(entity.latitude)
                            .setLongitude(entity.longitude)
                            .setAltitude(entity.altitude)
                            .setAccuracy(entity.accuracy)
                            .setSpeed(entity.speed)
                            .build()
            )
            is MediaEntity -> builder.setMedia(
                    DatumProto.Datum.Media.newBuilder()
                            .setMimeType(entity.mimeType)
                            .build()
            )
            is MessageEntity -> builder.setMessage(
                    DatumProto.Datum.Message.newBuilder()
                            .setNumber(entity.number)
                            .setMessageClass(entity.messageClass)
                            .setMessageBox(entity.messageBox)
                            .setContactType(entity.contactType)
                            .setIsStarred(entity.isStarred)
                            .setIsPinned(entity.isPinned)
                            .build()
            )
            is NotificationEntity -> builder.setNotification(
                    DatumProto.Datum.Notification.newBuilder()
                            .setName(entity.name)
                            .setPackageName(entity.packageName)
                            .setIsSystemApp(entity.isSystemApp)
                            .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                            .setTitle(entity.title)
                            .setVisibility(entity.visibility)
                            .setCategory(entity.category)
                            .setVibrate(entity.vibrate)
                            .setSound(entity.sound)
                            .setLightColor(entity.lightColor)
                            .setIsPosted(entity.isPosted)
                            .build()
            )
            is PhysicalStatEntity -> builder.setPhysicalStat(
                    DatumProto.Datum.PhysicalStat.newBuilder()
                            .setType(entity.type)
                            .setStartTime(entity.startTime)
                            .setEndTime(entity.endTime)
                            .setValue(entity.value)
                            .build()
            )
            is SurveyEntity -> builder.setSurvey(
                    DatumProto.Datum.Survey.newBuilder()
                            .setTitle(entity.title)
                            .setMessage(entity.message)
                            .setTimeoutPolicy(entity.timeoutPolicy)
                            .setTimeoutSec(entity.timeoutSec)
                            .setDeliveredTime(entity.deliveredTime)
                            .setReactionTime(entity.reactionTime)
                            .setResponseTime(entity.responseTime)
                            .setJson(entity.json)
                            .build()
            )
            is DataTrafficEntity -> builder.setDataTraffic(
                    DatumProto.Datum.DataTraffic.newBuilder()
                            .setFromTime(entity.fromTime)
                            .setToTime(entity.toTime)
                            .setRxBytes(entity.rxBytes)
                            .setTxBytes(entity.txBytes)
                            .setMobileRxBytes(entity.mobileRxBytes)
                            .setMobileTxBytes(entity.mobileTxBytes)
                            .build()
            )
            is WifiEntity -> builder.setWifi(
                    DatumProto.Datum.Wifi.newBuilder()
                            .setBssid(entity.bssid)
                            .setSsid(entity.ssid)
                            .setFrequency(entity.frequency)
                            .setRssi(entity.rssi)
                            .build()
            )
            else -> null
        }?.build()
    }

    companion object {
        private const val N_UPLOADS: Int = 100
        private val INTERVAL_SYNC = TimeUnit.HOURS.toMillis(1)
        private const val REQUEST_CODE_CANCEL_SYNC = 0x12
        private const val ACTION_CANCEL_SYNC = "${BuildConfig.APPLICATION_ID}.ACTION_CANCEL_SYNC "
        private val WORKER_NAME = SyncWorker2::class.java.name

        fun requestStart(context: Context, forceStart: Boolean, enableMetered: Boolean, isPeriodic: Boolean) {
            Log.d("SyncWorker", "requestStart(): forceStart = $forceStart, enableMetered = $enableMetered, isPeriodic = $isPeriodic")
            val manager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(if (enableMetered) NetworkType.NOT_ROAMING else NetworkType.UNMETERED)
                    .build()

            if (isPeriodic) {
                val request = PeriodicWorkRequestBuilder<SyncWorker2>(INTERVAL_SYNC, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .setInitialDelay(if (forceStart) 0L else INTERVAL_SYNC, TimeUnit.MILLISECONDS)
                        .build()

                manager.enqueueUniquePeriodicWork(
                        WORKER_NAME,
                        if (forceStart) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
                        request
                )
            } else {
                if (forceStart) {
                    val request = OneTimeWorkRequestBuilder<SyncWorker2>()
                            .setConstraints(constraints)
                            .build()

                    manager.enqueueUniqueWork(
                            WORKER_NAME,
                            ExistingWorkPolicy.REPLACE,
                            request
                    )
                } else {
                    requestStop(context)
                }
            }
        }

        fun requestStop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_NAME)
        }
    }
}
