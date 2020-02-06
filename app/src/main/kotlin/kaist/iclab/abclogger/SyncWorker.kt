package kaist.iclab.abclogger

import android.content.Context
import android.util.Log
import androidx.work.*
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import io.objectbox.EntityInfo
import io.objectbox.query.Query
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
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity_
import kaist.iclab.abclogger.collector.traffic.DataTrafficEntity
import kaist.iclab.abclogger.collector.wifi.WifiEntity
import kaist.iclab.abclogger.grpc.DataOperationsCoroutineGrpc
import kaist.iclab.abclogger.grpc.DatumProto
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit


class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val foregroundInfo = ForegroundInfo(
            Notifications.ID_SYNC_PROGRESS,
            Notifications.build(
                    context = applicationContext,
                    channelId = Notifications.CHANNEL_ID_PROGRESS,
                    title = applicationContext.getString(R.string.ntf_title_sync),
                    text = applicationContext.getString(R.string.ntf_text_sync),
                    progress = 0,
                    indeterminate = true
            )
    )

    override suspend fun doWork(): Result {
        setForeground(foregroundInfo)

        val channel: ManagedChannel = AndroidChannelBuilder
                .forTarget(BuildConfig.SERVER_ADDRESS)
                .usePlaintext()
                .context(applicationContext)
                .executor(Dispatchers.IO.asExecutor())
                .build()

        val stub: DataOperationsCoroutineGrpc.DataOperationsCoroutineStub = DataOperationsCoroutineGrpc.newStubWithContext(channel)
                .withDeadlineAfter(10, TimeUnit.SECONDS)

        uploadAll(stub)
        removeAll()

        Prefs.lastTimeDataSync = System.currentTimeMillis()

        terminate(channel)

        return Result.success()
    }

    private suspend fun uploadAll(stub: DataOperationsCoroutineGrpc.DataOperationsCoroutineStub) = withContext(Dispatchers.IO) {
        ObjBox.boxStore.get().allEntityClasses.forEach { clazz ->
            try {
                val query = query(clazz) ?: throw Exception("No corresponding query")
                val count = query.count()

                (0 until count step N_UPLOADS).forEach { offset ->
                    val entities = if (clazz == SurveyEntity::class.java) {
                        query.find(offset, N_UPLOADS).filter { entity -> (entity as? SurveyEntity)?.isAvailable() == false }
                    } else {
                        query.find(offset, N_UPLOADS)
                    }
                    val deferred = entities.map { entity ->
                        async {
                            try {
                                toProto(entity)?.let { stub.createDatum(it) }
                                entity
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    val uploaded = deferred.awaitAll().filterNotNull()
                    uploaded.forEach { entity -> (entity as? Base)?.isUploaded = true }
                    ObjBox.put(uploaded)
                }
            } catch (e: Exception) {
                AppLog.ee(e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun removeAll() = withContext(Dispatchers.IO) {
        ObjBox.boxStore.get().allEntityClasses.forEach { clazz ->
            try {
                remove(clazz)
            } catch (e: Exception) {
                AppLog.ee(e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun terminate(channel: ManagedChannel) = withContext(Dispatchers.IO) {
        try {
            channel.shutdownNow().awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> query(clazz: Class<T>): Query<T>? {
        val box = ObjBox.boxFor(clazz) ?: return null
        val properties = (box.entityInfo as? EntityInfo<T>)?.allProperties ?: return null
        val isUploaded = properties.find { property -> property.name == "isUploaded" }
                ?: return null
        return box.query().equal(isUploaded, false).build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> remove(clazz: Class<T>) {
        val box = ObjBox.boxFor(clazz) ?: return
        val properties = (box.entityInfo as? EntityInfo<T>)?.allProperties ?: return
        val isUploaded = properties.find { property -> property.name == "isUploaded" } ?: return

        box.query().equal(isUploaded, true).build().remove()
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
                            .setPrevKey(entity.prevKey)
                            .setCurrentKey(entity.currentKey)
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
        private const val N_UPLOADS: Long = 100
        private val INTERVAL_SYNC = TimeUnit.HOURS.toMillis(1)

        fun requestStart(context: Context, forceStart: Boolean, enableMetered: Boolean? = null) {
            if (enableMetered != null) Prefs.canUploadMeteredNetwork = enableMetered

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(if (Prefs.canUploadMeteredNetwork) NetworkType.NOT_ROAMING else NetworkType.UNMETERED)
                    .build()

            val initDelay = if (forceStart) 0L else INTERVAL_SYNC

            val request = PeriodicWorkRequestBuilder<SyncWorker>(INTERVAL_SYNC, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .setInitialDelay(initDelay, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SyncWorker::class.java.name,
                    if (forceStart) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
                    request
            )
        }

        fun requestStop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SyncWorker::class.java.name)
        }
    }
}
