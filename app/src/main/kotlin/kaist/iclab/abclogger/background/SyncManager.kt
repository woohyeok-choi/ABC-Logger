package kaist.iclab.abclogger.background

import android.app.Activity
import android.app.ActivityManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import com.google.gson.GsonBuilder
import io.objectbox.Box
import io.objectbox.EntityInfo
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.common.NoParticipatedExperimentException
import kaist.iclab.abclogger.common.util.FunctionUtils
import kaist.iclab.abclogger.common.util.NotificationUtils
import kaist.iclab.abclogger.common.util.WorkerUtils
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.exportSQLite
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.getExported
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.writeStringData
import kaist.iclab.abclogger.data.PreferenceAccessor
import kaist.iclab.abclogger.data.entities.*
import kaist.iclab.abclogger.prefs
import java.io.File



object SyncManager {
    val TAG: String = SyncManager::class.java.simpleName

    private const val LIMIT: Long = 60
    private const val THREE_DAYS_IN_MS: Long = 1000 * 60 * 60 * 24 * 3
    private const val TEN_DAYS_IN_MS: Long = 1000 * 60 * 60 * 24 * 10
    private const val TWELVE_HOURS_IN_MS: Long = 1000 * 60 * 60 * 12
    private const val SIX_HOURS_IN_MS: Long = 1000 * 60 * 60 * 6
    private const val THREE_HOURS_IN_MS: Long = 1000 * 60 * 60 * 3
    private const val SERVICE_NAME_ABCLOGGER = "kaist.iclab.abclogger.background.collector.NotificationCollector"
    private const val SERVICE_NAME_ABCLOGGER2 = "kaist.iclab.abclogger.background.CollectorService"
    private const val SERVICE_NAME_MSBAND = "iclab.kaist.ac.kr.msband_logger.Service.AccessService"
    private const val SERVICE_NAME_POLAR = "fi.polar.beat.service.ExerciseService"
    private const val SERVICE_NAME_PACO = "com.pacoapp.paco.net.SyncService"
    private var msBandFlag = false
    private var polarFlag = false
    private var pacoFlag = false
    private var abcFlag = false


    fun sync(isForced: Boolean = false) {
        WorkerUtils.startPeriodicWorkerAsync<SyncWorker>(1000 * 60 * 15, isForced)
    }

    fun syncWithProgressShown(context: Context) {
        context.startService(Intent(context, SyncService::class.java))
    }


    class SyncWorker: Worker() {
        override fun doWork(): Result {
            sync(applicationContext, false)
            return Result.SUCCESS
        }
    }

    class SyncService: IntentService(TAG) {
        override fun onHandleIntent(intent: Intent?) {
            sync(this, true)
        }
    }

    private fun sync(context: Context, showProgress: Boolean) {
        val pref = PreferenceAccessor.getInstance(context)
        pref.isSyncInProgress = true

        try {
            ParticipationEntity.getParticipatedExperimentFromServer(context)
        } catch (e: NoParticipatedExperimentException) {
            Log.d(TAG, e.toString())
            pref.clear()
        } catch (e: Exception) { }

        try {
            val unitProgress = 100 / 24
            var progress = 0
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * progress, 100)

            uploadLogs(context)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<AppUsageEventEntity>(), AppUsageEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<AppUsageStatEntity>(), AppUsageStatEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<BatteryEntity>(), BatteryEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<CallLogEntity>(), CallLogEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<ConnectivityEntity>(), ConnectivityEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<DataTrafficEntity>(), DataTrafficEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<DeviceEventEntity>(), DeviceEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<EmotionalStatusEntity>(), EmotionalStatusEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<InstalledAppEntity>(), InstalledAppEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<LocationEntity>(), LocationEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<MediaEntity>(), MediaEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<MessageEntity>(), MessageEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<NotificationEntity>(), NotificationEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalActivityEventEntity>(), PhysicalActivityEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalStatusEntity>(), PhysicalStatusEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalActivityTransitionEntity>(), PhysicalActivityTransitionEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<RecordEntity>(), RecordEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<SensorEntity>(), SensorEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            ///*
            uploadEntity(context, App.boxFor<WeatherEntity>(), WeatherEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)
            //*/

            uploadEntity(context, App.boxFor<WifiEntity>(), WifiEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<BluetoothDeviceEntity>(), BluetoothDeviceEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context, unitProgress * (++progress), 100)

            ///*
            uploadEntity(context, App.boxFor<SurveyEntity>(), SurveyEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context, 0, 0)
            //*/

            /**
             * DashBoard Sync
             */

            /*
            checkServiceRunning(context = context)

            val uuid = prefs.participantPhoneNumber!!
            val group = prefs.participantGroup!!
            val postMsg = PhpApi.dataToByteArray(uuid, group, msBandFlag = msBandFlag, pacoFlag = pacoFlag, polarFlag = polarFlag, abcFlag = abcFlag)
            val resultTask = PhpApi.request(context, "http://143.248.90.57/server.php", postMsg)

            resultTask.addOnSuccessListener {
                Log.d(TAG, "Dashboard sync success: $it")
            }.addOnFailureListener {
                Log.d(TAG, "Dashboard sync failed")
            }
            */
            pref.lastTimeSynced = System.currentTimeMillis()

            ///*
            Log.d(TAG, "getExported(): ${getExported()}, lastTimeExportDB: ${prefs.lastTimeExportDB}")
            if (!getExported() &&
                    (System.currentTimeMillis() - prefs.lastTimeExportDB > THREE_HOURS_IN_MS)) {
                exportSQLite(context, "")
                prefs.lastTimeExportDB = System.currentTimeMillis()
            }
            //*/
        } catch (e: Exception) {
            if(showProgress) NotificationUtils.notifyUploadProgress(context, 0, 0, e)
        } finally {
            pref.isSyncInProgress = false
        }
    }

    private fun uploadLogs(context: Context) {
        val box = App.boxFor<LogEntity>()
        val uploadQuery = box.query()
            .equal(LogEntity_.isUploaded, false)
            .build()

        while(uploadQuery.count() > 0) {
            uploadQuery.find(0, LIMIT).let { entities ->
                // GrpcApi.uploadLog(entities)
                entities.forEach {
                    it.isUploaded = true

                    /**
                     * SW EDIT - write data to database
                     */
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val jsonEntity: String = gson.toJson(it)
                    writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
                }
                box.put(entities)
                /**
                 *  uploadQuery 는 껍데기일뿐이다.
                 *  .find () 등으로 실행시켜야 의미를 지닌다.
                 *  따라서 box.put(entities) 시점에서 매번 box 내용물이 갱신된다고 보면된다.
                 **/
            }
        }

        val removeQuery = box.query()
            .equal(LogEntity_.isUploaded, true)
            .build()
        removeQuery.remove()
    }

    private inline fun <reified T: BaseEntity> uploadEntity(context: Context, box: Box<T>, info: EntityInfo<T>) {
        /*
        if(!NetworkUtils.isWifiAvailable(context)) {
            throw NoWifiNetworkAvailableException()
        }
        */
        Log.d(TAG, "uploadEntity: ${T::class.java.simpleName}")

        FunctionUtils.runIfAllNotNull(
            info.allProperties.find { it.name == "timestamp" },
            info.allProperties.find { it.name == "isUploaded" }
        ) { timestampProperty, isUploadedProperty ->
            val uploadQuery = box.query()
                .greater(timestampProperty, 0)
                .and()
                .equal(isUploadedProperty, false)
                .build()

            while (uploadQuery.count() > 0) {
                uploadQuery.find(0, LIMIT).let { entities ->
                    //GrpcApi.uploadEntities(entities)
                    entities.forEach {
                        it.isUploaded = true

                        /**
                         * SW EDIT - write data to database
                         */
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        val jsonEntity: String = gson.toJson(it)
                        writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
                    }
                    box.put(entities)
                    /**
                     *  uploadQuery 는 껍데기일뿐이다.
                     *  .find () 등으로 실행시켜야 의미를 지닌다.
                     *  따라서 box.put(entities) 시점에서 매번 box 내용물이 갱신된다고 보면된다.
                     **/
                }
            }

            if(T::class.java == SurveyEntity::class.java) return@runIfAllNotNull

            val removeQuery = box.query()
                .less(timestampProperty, System.currentTimeMillis() - TEN_DAYS_IN_MS)
                .and()
                .equal(isUploadedProperty, true)
                .build()

            removeQuery.forEach {
                if(it is RecordEntity) {
                    val file = File(it.path)
                    if(file.exists()) file.delete()
                    /**
                     *  녹음은 했는데, 음성이 재생될 무언가가 아니다.
                     */
                }
            }
            removeQuery.remove()
        }
    }

    private fun checkServiceRunning(context: Context) {
        msBandFlag = false  // 앱 accessibility 안켜면 실행 안됨;
        polarFlag = false   // 폴라앱 끄면 실행 안됨;
        pacoFlag = false    // 이거 그냥 무조건 실행되고 있음; 문제 있음;
        abcFlag = false     // not tested
        Log.d(TAG, "checkServiceRunning ()")
        val am = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        am.getRunningServices(Integer.MAX_VALUE).forEach {
            Log.i(TAG, it.service.className)
            when (it.service.className) {
                SERVICE_NAME_ABCLOGGER -> abcFlag = true
                SERVICE_NAME_ABCLOGGER2 -> abcFlag = true
                SERVICE_NAME_MSBAND -> msBandFlag = true
                SERVICE_NAME_PACO -> pacoFlag = true
                SERVICE_NAME_POLAR -> polarFlag = true
            }
        }
    }
}
