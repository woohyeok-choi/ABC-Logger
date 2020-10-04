package kaist.iclab.abclogger.core.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.*
import io.grpc.StatusRuntimeException
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.activity.PhysicalActivityEntity
import kaist.iclab.abclogger.collector.appusage.AppUsageEventEntity
import kaist.iclab.abclogger.collector.battery.BatteryEntity
import kaist.iclab.abclogger.collector.bluetooth.BluetoothEntity
import kaist.iclab.abclogger.collector.call.CallLogEntity
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.install.InstalledAppEntity
import kaist.iclab.abclogger.collector.keylog.KeyLogEntity
import kaist.iclab.abclogger.collector.location.LocationEntity
import kaist.iclab.abclogger.collector.notification.NotificationEntity
import kaist.iclab.abclogger.collector.fitness.FitnessEntity
import kaist.iclab.abclogger.collector.embedded.EmbeddedSensorEntity
import kaist.iclab.abclogger.collector.external.ExternalSensorEntity
import kaist.iclab.abclogger.collector.media.MediaEntity
import kaist.iclab.abclogger.collector.message.MessageEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.traffic.DataTrafficEntity
import kaist.iclab.abclogger.collector.transition.PhysicalActivityTransitionEntity
import kaist.iclab.abclogger.collector.wifi.WifiEntity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.CollectorRepository
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.NotificationRepository
import kaist.iclab.abclogger.core.Preference
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.grpc.service.DataOperationsGrpcKt
import kaist.iclab.abclogger.grpc.service.ServiceProtos
import kaist.iclab.abclogger.grpc.proto.DatumProtos
import kaist.iclab.abclogger.grpc.proto.SubjectProtos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit

class SyncRepository(context: Context, params: WorkerParameters) :
    AbstractNetworkRepository<DataOperationsGrpcKt.DataOperationsCoroutineStub>(context, params),
    KoinComponent {
    private val collectorRepository: CollectorRepository by inject()
    private val cancelIntent by lazy {
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
    }

    override val stub: DataOperationsGrpcKt.DataOperationsCoroutineStub by lazy {
        DataOperationsGrpcKt.DataOperationsCoroutineStub(channel)
    }

    override suspend fun doWork(): Result {
        if (!checkNetworkConstraints()) return Result.retry()

        setForeground(NotificationRepository.syncInitialize(applicationContext, cancelIntent))

        val throwable: Throwable? = withContext(dispatcher) {
            val totalCount = collectorRepository.countLocalRecords()
            var totalUploadCount = 0L

            if (totalCount <= 0) return@withContext null

            for (collector in collectorRepository.all) {
                val throwable = upload(SIZE_BULK, collector) { size ->
                    totalUploadCount += size
                    val progress = NotificationRepository.syncProgress(
                        context = applicationContext,
                        max = totalCount,
                        progress = totalUploadCount,
                        cancelIntent = cancelIntent
                    )
                    setForeground(progress)
                }

                if (throwable != null) return@withContext throwable
            }

            return@withContext null
        }

        shutdown()

        return if (throwable == null) {
            setForeground(NotificationRepository.syncSuccess(applicationContext))
            Preference.lastTimeDataSync = System.currentTimeMillis()
            Result.success()
        } else {
            val isRetry = isRetryableException(throwable)
            val wrapException = when (throwable) {
                is StatusRuntimeException -> SyncError.fromStatusRuntimeException(throwable)
                else -> throwable
            }
            setForeground(NotificationRepository.syncFailure(applicationContext, wrapException))
            Log.e(javaClass, wrapException, report = true)

            if (isRetry) Result.retry() else Result.failure()
        }
    }

    private fun <T : AbstractEntity> toDatum(entities: Collection<T>) = entities.map { entity ->
        proto(DatumProtos.Datum.newBuilder()) {
            timestamp = entity.timestamp
            utcOffsetSec = entity.utcOffset
            uploadTime = System.currentTimeMillis()
            subject = proto(SubjectProtos.Subject.newBuilder()) {
                groupName = entity.groupName
                email = entity.email
                instanceId = entity.instanceId
                source = entity.source
                deviceManufacturer = entity.deviceManufacturer
                deviceModel = entity.deviceModel
                deviceVersion = entity.deviceVersion
                deviceOs = entity.deviceOs
                appId = entity.appId
                appVersion = entity.appVersion
            }

            when (entity) {
                is PhysicalActivityEntity -> physicalActivity = toProto(entity)
                is PhysicalActivityTransitionEntity -> physicalActivityTransition = toProto(entity)
                is AppUsageEventEntity -> appUsageEvent = toProto(entity)
                is BatteryEntity -> battery = toProto(entity)
                is BluetoothEntity -> bluetooth = toProto(entity)
                is CallLogEntity -> callLog = toProto(entity)
                is DeviceEventEntity -> deviceEvent = toProto(entity)
                is EmbeddedSensorEntity -> embeddedSensor = toProto(entity)
                is ExternalSensorEntity -> externalSensor = toProto(entity)
                is InstalledAppEntity -> installedApp = toProto(entity)
                is KeyLogEntity -> keyLog = toProto(entity)
                is LocationEntity -> location = toProto(entity)
                is MediaEntity -> media = toProto(entity)
                is MessageEntity -> message = toProto(entity)
                is NotificationEntity -> notification = toProto(entity)
                is FitnessEntity -> fitness = toProto(entity)
                is SurveyEntity -> survey = toProto(entity)
                is DataTrafficEntity -> dataTraffic = toProto(entity)
                is WifiEntity -> wifi = toProto(entity)
            }
        }
    }

    private fun checkNetworkConstraints(): Boolean {
        if (!isNetworkAvailable(applicationContext)) {
            return false
        }

        if (!isNonMeteredNetworkAvailable(applicationContext) && Preference.isSyncableWithWifiOnly) {
            return false
        }

        return true
    }

    @SuppressLint("CheckResult")
    private suspend fun <T : AbstractCollector<E>, E : AbstractEntity> upload(
        bulkSize: Long,
        collector: T,
        block: suspend (Long) -> Unit
    ): Throwable? {
        val totalSize = collector.count()
        var uploadSize = 0L

        val deadlineStub = stub.withDeadlineAfter(DEADLINE_STUB, TimeUnit.MILLISECONDS)

        while (uploadSize < totalSize) {
            val entities = collector.list(bulkSize)
            val size = entities.size

            if (size == 0) break

            val bulkProto = proto(ServiceProtos.Bulk.Data.newBuilder()) {
                addAllDatum(toDatum(entities))
            }

            val throwable = upload(bulkProto, deadlineStub)
            if (throwable != null) {
                return throwable
            }

            collector.flush(entities)

            uploadSize += size
            block.invoke(size.toLong())
        }

        return null
    }

    @SuppressLint("CheckResult")
    private suspend fun upload(
        bulkProto: ServiceProtos.Bulk.Data,
        stub: DataOperationsGrpcKt.DataOperationsCoroutineStub
    ): Throwable? {
        var trial = 0
        var throwable: Throwable? = null

        while (trial < MAX_RETRY_ATTEMPTS) {
            try {
                trial++

                if (!checkNetworkConstraints()) throw SyncError.networkStatusChanged()

                throwable = null

                stub.createData(bulkProto)
                break
            } catch (e: Exception) {
                throwable = e

                if (!isRetryableException(e)) break
            }

            delay(INTERVAL_RETRY)
        }

        return throwable
    }

    private fun isRetryableException(throwable: Throwable) = when (throwable) {
        is SyncError -> throwable.isRetry
        is StatusRuntimeException -> SyncError.fromStatusRuntimeException(throwable).isRetry
        else -> false
    }

    private fun toProto(entity: PhysicalActivityEntity) =
        proto(DatumProtos.PhysicalActivity.newBuilder()) {
            val activities = entity.activities.map { activity ->
                proto(DatumProtos.PhysicalActivity.Activity.newBuilder()) {
                    type = activity.type
                    confidence = activity.confidence
                }
            }
            addAllActivity(activities)
        }

    private fun toProto(entity: PhysicalActivityTransitionEntity) =
        proto(DatumProtos.PhysicalActivityTransition.newBuilder()) {
            isEntered = entity.isEntered
            type = entity.type
        }

    private fun toProto(entity: AppUsageEventEntity) =
        proto(DatumProtos.AppUsageEvent.newBuilder()) {
            name = entity.name
            packageName = entity.packageName
            type = entity.type
            isSystemApp = entity.isSystemApp
            isUpdatedSystemApp = entity.isUpdatedSystemApp
        }

    private fun toProto(entity: BatteryEntity) = proto(DatumProtos.Battery.newBuilder()) {
        level = entity.level
        scale = entity.scale
        temperature = entity.temperature
        voltage = entity.voltage
        health = entity.health
        pluggedType = entity.pluggedType
        status = entity.status
        capacity = entity.capacity
        chargeCounter = entity.chargeCounter
        currentAverage = entity.currentAverage
        currentNow = entity.currentNow
        energyCounter = entity.energyCounter
        technology = entity.technology
    }

    private fun toProto(entity: BluetoothEntity) = proto(DatumProtos.Bluetooth.newBuilder()) {
        name = entity.name
        alias = entity.alias
        address = entity.address
        bondState = entity.bondState
        deviceType = entity.deviceType
        classType = entity.classType
        rssi = entity.rssi
        isLowEnergy = entity.isLowEnergy
    }

    private fun toProto(entity: CallLogEntity) = proto(DatumProtos.CallLog.newBuilder()) {
        duration = entity.duration
        number = entity.number
        type = entity.type
        presentation = entity.presentation
        dataUsage = entity.dataUsage
        contactType = entity.contactType
        isStarred = entity.isStarred
        isPinned = entity.isPinned
    }

    private fun toProto(entity: DeviceEventEntity) = proto(DatumProtos.DeviceEvent.newBuilder()) {
        type = entity.eventType
        putAllExtra(entity.extras)
    }

    private fun toProto(entity: EmbeddedSensorEntity) =
        proto(DatumProtos.EmbeddedSensor.newBuilder()) {
            valueType = entity.valueType
            putAllStatus(entity.status)
            valueFormat = entity.valueFormat
            valueUnit = entity.valueUnit
            addAllValue(entity.values)
        }

    private fun toProto(entity: ExternalSensorEntity) =
        proto(DatumProtos.ExternalSensor.newBuilder()) {
            deviceType = entity.deviceType
            valueType = entity.valueType
            identifier = entity.identifier
            putAllStatus(entity.status)
            valueFormat = entity.valueFormat
            valueUnit = entity.valueUnit
            addAllValue(entity.values)
        }

    private fun toProto(entity: InstalledAppEntity) = proto(DatumProtos.InstalledApp.newBuilder()) {
        val apps = entity.apps.map { app ->
            proto(DatumProtos.InstalledApp.App.newBuilder()) {
                name = app.name
                packageName = app.packageName
                isSystemApp = app.isSystemApp
                isUpdatedSystemApp = app.isUpdatedSystemApp
                firstInstallTime = app.firstInstallTime
                lastUpdateTime = app.lastUpdateTime
            }
        }
        addAllApp(apps)
    }

    private fun toProto(entity: KeyLogEntity) = proto(DatumProtos.KeyLog.newBuilder()) {
        name = entity.name
        packageName = entity.packageName
        isSystemApp = entity.isSystemApp
        isUpdatedSystemApp = entity.isUpdatedSystemApp
        distance = entity.distance
        timeTaken = entity.timeTaken
        keyboardType = entity.keyboardType
        prevKey = entity.prevKey
        currentKey = entity.currentKey
        prevKeyType = entity.prevKeyType
        currentKeyType = entity.currentKeyType
    }

    private fun toProto(entity: LocationEntity) = proto(DatumProtos.Location.newBuilder()) {
        latitude = entity.latitude
        longitude = entity.longitude
        altitude = entity.altitude
        accuracy = entity.accuracy
        speed = entity.speed
    }

    private fun toProto(entity: MediaEntity) = proto(DatumProtos.Media.newBuilder()) {
        mimeType = entity.mimeType
    }

    private fun toProto(entity: MessageEntity) = proto(DatumProtos.Message.newBuilder()) {
        number = entity.number
        messageClass = entity.messageClass
        messageBox = entity.messageBox
        contactType = entity.contactType
        isStarred = entity.isStarred
        isPinned = entity.isPinned
    }

    private fun toProto(entity: NotificationEntity) = proto(DatumProtos.Notification.newBuilder()) {
        name = entity.name
        packageName = entity.packageName
        isSystemApp = entity.isSystemApp
        isUpdatedSystemApp = entity.isUpdatedSystemApp
        title = entity.title
        bigTitle = entity.bigTitle
        title = entity.title
        subText = entity.subText
        bigText = entity.bigText
        summaryText = entity.summaryText
        infoText = entity.infoText
        visibility = entity.visibility
        category = entity.category
        priority = entity.priority
        vibrate = entity.vibrate
        sound = entity.sound
        lightColor = entity.lightColor
        isPosted = entity.isPosted
    }

    private fun toProto(entity: FitnessEntity) = proto(DatumProtos.Fitness.newBuilder()) {
        type = entity.type
        startTime = entity.startTime
        endTime = entity.endTime
        value = entity.value
        fitnessDeviceModel = entity.fitnessDeviceModel
        fitnessDeviceManufacturer = entity.fitnessDeviceManufacturer
        fitnessDeviceType = entity.fitnessDeviceType
        dataSourceName = entity.dataSourceName
        dataSourcePackageName = entity.dataSourcePackageName
    }

    private fun toProto(entity: SurveyEntity) = proto(DatumProtos.Survey.newBuilder()) {
        eventTime = entity.eventTime
        eventName = entity.eventName
        intendedTriggerTime = entity.intendedTriggerTime
        actualTriggerTime = entity.actualTriggerTime
        reactionTime = entity.reactionTime
        responseTime = entity.responseTime
        url = entity.url
        title = entity.title
        altTitle = entity.altTitle
        message = entity.message
        altMessage = entity.altMessage
        instruction = entity.instruction
        altInstruction = entity.altInstruction
        timeoutUntil = entity.timeoutUntil
        timeoutAction = entity.timeoutAction

        val responses = entity.responses.map { response ->
            proto(DatumProtos.Survey.Response.newBuilder()) {
                index = response.index
                type = response.type
                question = response.question
                altQuestion = response.altQuestion
                addAllAnswer(response.answer)
            }
        }
        addAllResponse(responses)
    }

    private fun toProto(entity: DataTrafficEntity) = proto(DatumProtos.DataTraffic.newBuilder()) {
        fromTime = entity.fromTime
        toTime = entity.toTime
        rxBytes = entity.rxBytes
        txBytes = entity.txBytes
        mobileRxBytes = entity.mobileRxBytes
        mobileTxBytes = entity.mobileTxBytes
    }

    private fun toProto(entity: WifiEntity) = proto(DatumProtos.Wifi.newBuilder()) {
        val accessPoints = entity.accessPoints.map { ap ->
            proto(DatumProtos.Wifi.AccessPoint.newBuilder()) {
                bssid = ap.bssid
                ssid = ap.ssid
                frequency = ap.frequency
                rssi = ap.rssi
            }
        }
        addAllAccessPoint(accessPoints)
    }

    companion object {
        private const val SIZE_BULK = 500L
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INTERVAL_RETRY = 1000L * 5
        private const val DEADLINE_STUB = 1000L * 60 * 5
        private const val INTERVAL_MINUTE = 60L

        private const val NAME_PERIODIC_WORKER =
            "${BuildConfig.APPLICATION_ID}.core.SyncRepository.PeriodicWorker"
        private const val NAME_ONETIME_WORKER =
            "${BuildConfig.APPLICATION_ID}.core.SyncRepository.OneTimeWorker"
        private const val TAG_SYNC_WORKER =
            "${BuildConfig.APPLICATION_ID}.core.SyncRepository.SyncWorker"

        private fun getPeriodicRequest(delayInMinute: Long) =
            PeriodicWorkRequestBuilder<SyncRepository>(INTERVAL_MINUTE, TimeUnit.MINUTES)
                .setInitialDelay(delayInMinute, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .addTag(TAG_SYNC_WORKER)
                .build()

        private fun getOneTimeRequest(delayInMinute: Long) =
            OneTimeWorkRequestBuilder<SyncRepository>()
                .setInitialDelay(delayInMinute, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .addTag(TAG_SYNC_WORKER)
                .build()

        /**
         * Sync with coroutine worker;
         * @return Flow<Boolean> which means complete or not.
         */
        private suspend fun sync(
            context: Context,
            isPeriodic: Boolean,
            initialDelay: Long
        ): Flow<Operation.State>? {
            val manager = WorkManager.getInstance(context)
            val isIdle = manager.getWorkInfosForUniqueWork(
                if (isPeriodic) NAME_PERIODIC_WORKER else NAME_ONETIME_WORKER
            ).await().find {
                it.state == WorkInfo.State.RUNNING
            } == null

            if (!isIdle) return null

            val operation = if (isPeriodic) {
                manager.enqueueUniquePeriodicWork(
                    NAME_PERIODIC_WORKER,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    getPeriodicRequest(initialDelay)
                )
            } else {
                manager.enqueueUniqueWork(
                    NAME_ONETIME_WORKER,
                    ExistingWorkPolicy.REPLACE,
                    getOneTimeRequest(initialDelay)
                )
            }
            return operation.state.asFlow()
        }

        suspend fun syncNow(context: Context, isPeriodic: Boolean) =
            sync(context, isPeriodic, 0)

        suspend fun setAutoSync(context: Context, isEnabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            manager.cancelAllWorkByTag(TAG_SYNC_WORKER).await()
            if (isEnabled) sync(context, true, INTERVAL_MINUTE)
        }
    }
}

