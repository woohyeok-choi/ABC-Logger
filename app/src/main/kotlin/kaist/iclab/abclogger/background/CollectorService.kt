package kaist.iclab.abclogger.background

import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.base.BaseService
import kaist.iclab.abclogger.common.util.NotificationUtils
import kaist.iclab.abclogger.data.entities.LogEntity
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.prefs

class CollectorService: BaseService() {
    companion object {
        val EXTRA_STOP_SERVICE = "${CollectorService::class.java.canonicalName}.EXTRA_STOP_SERVICE"
    }

    private lateinit var collectors: MutableMap<String, BaseCollector>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        collectors = mutableMapOf()

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(SurveyCollector.EventReceiver,
            IntentFilter(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogEntity.log(TAG, "startForeground")
        startForeground(
            NotificationUtils.NOTIFICATION_ID_EXPERIMENT_IN_PROGRESS,
            NotificationUtils.buildNotificationForExperimentInProgress(this)
        )

        if(intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) == true) {
            LogEntity.log(TAG, "stopSelf: $startId")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        try {
            LogEntity.log(TAG, "participant service start")

            /* 여기서 문제 발생 */
            // val entity = ParticipationEntity.getParticipatedExperimentFromLocal()

            /* temporal entity create */
            //val entity = ParticipationEntity.getParticipationFromLocal()
            //entity.requiresAppUsage = true
            //entity.subjectEmail = "sw.kang@kaist.ac.kr"

            if(prefs.requiresAmbientSound && !exists<AmbientSoundCollector>()){
                put(AmbientSoundCollector( this))
                LogEntity.log(TAG, "AmbientSoundCollector start")
            } else { LogEntity.log(TAG, "AmbientSoundCollector not start") }
            if(prefs.requiresLocationAndActivity && !exists<LocationAndActivityCollector>()) {
                put(LocationAndActivityCollector( this))
                LogEntity.log(TAG, "LocationAndActivityCollector start")
            } else { LogEntity.log(TAG, "LocationAndActivityCollector not start") }

            if(prefs.requiresGoogleFitness && !exists<GoogleFitnessCollector>()) {
                put(GoogleFitnessCollector( this))
                LogEntity.log(TAG, "GoogleFitnessCollector start")
            } else { LogEntity.log(TAG, "GoogleFitnessCollector not start") }
            if(prefs.requiresEventAndTraffic && !exists<DeviceEventAndTrafficCollector>()) {
                put(DeviceEventAndTrafficCollector( this))
                LogEntity.log(TAG, "DeviceEventAndTrafficCollector start ")
            } else { LogEntity.log(TAG, "DeviceEventAndTrafficCollector not start") }

            if(prefs.requiresContentProviders && !exists<ContentProviderCollector>()) {
                put(ContentProviderCollector( this))
                LogEntity.log(TAG, "ContentProviderCollector start")
            } else { LogEntity.log(TAG, "ContentProviderCollector not start") }

            if(prefs.requiresAppUsage && !exists<AppUsageCollector>()) {
                put(AppUsageCollector( this))
                LogEntity.log(TAG, "AppUsageCollector start")
            } else { LogEntity.log(TAG, "AppUsageCollector not start") }
            //if(!exists<WeatherCollector>()) put(WeatherCollector( this))
            //LogEntity.log(TAG, "Weather collector")

            if(!exists<BluetoothCollector2>()) {
                put(BluetoothCollector2(this))
                LogEntity.log(TAG, "BluetoothCollector start")
            } else { LogEntity.log(TAG, "BluetoothCollector not start") }

            //start(entity)
            start(id = prefs.participantPhoneNumber!!,
                    expGroup = prefs.participantGroup!!,
                    expEmail = prefs.participantEmail!!)
            LogEntity.log(TAG, "onStartCommand()")
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stop()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(SurveyCollector.EventReceiver)
        LogEntity.log(TAG, "onDestroy()")
        super.onDestroy()
    }

    private inline fun <reified T : BaseCollector> exists() = collectors[T::class.java.name] != null

    private inline fun <reified T : BaseCollector> put(collector : T) {
        var temp = T::class.java.name
        LogEntity.log(TAG, "put: $temp")
        collectors[T::class.java.name] = collector
    }

    private fun start(entity: ParticipationEntity) {
        LogEntity.log(TAG, "start collectors")
        collectors.values.forEach { it.startCollection(uuid = entity.experimentUuid, group = entity.experimentGroup, email = entity.subjectEmail) }
    }

    private fun start(id: String, expGroup: String, expEmail: String) {
        LogEntity.log(TAG, "start my collectors")
        collectors.values.forEach { it.startCollection(uuid = id, group = expGroup, email = expEmail) }
    }

    private fun stop() {
        LogEntity.log(TAG, "stop collectors")
        collectors.values.forEach { it.stopCollection() }
        collectors.clear()
    }
}



