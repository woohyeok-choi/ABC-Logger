package kaist.iclab.abclogger.background.collector

import androidx.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.entities.*
import kaist.iclab.abclogger.data.types.BatteryPluggedType
import kaist.iclab.abclogger.data.types.BatteryStatusType
import kaist.iclab.abclogger.data.types.ConnectivityNetworkType
import kaist.iclab.abclogger.data.types.DeviceEventType
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * This collector handles device event and data traffic, which does not requires any runtime permissions.
 */
class DeviceEventAndTrafficCollector(val context: Context) : BaseCollector {
    private var scheduledFutureForTraffic: ScheduledFuture<*>? = null
    private var prevRxBytes: Long? = null
    private var prevTxBytes: Long? = null
    private var currentScreenState = DeviceEventType.SCREEN_ON

    private val intentFilter = IntentFilter().apply {
        arrayOf(
            Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_SHUTDOWN,
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
            AudioManager.RINGER_MODE_CHANGED_ACTION,
            ConnectivityManager.CONNECTIVITY_ACTION,
            Intent.ACTION_BATTERY_CHANGED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY
        ).forEach { addAction(it) }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(context == null || intent == null) return

            extractConnectivityEntity(intent)?.let {
                App.boxFor<ConnectivityEntity>().put(it)
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* Connectivity - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            extractBatteryEntity(intent)?.let {
                App.boxFor<BatteryEntity>().put(it)
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* Battery - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            extractDeviceEventEntity(context, intent)?.let {
                App.boxFor<DeviceEventEntity>().put(it)
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* Device Event - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)

                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(
                    Intent(ACTION_DEVICE_EVENT_CHANGE).putExtra(EXTRA_DEVICE_EVENT, it.type.id)
                )

                if(it.type == DeviceEventType.SCREEN_ON || it.type == DeviceEventType.SCREEN_OFF) {
                    currentScreenState = it.type
                }
            }
        }
    }

    private lateinit var uuid: String
    private lateinit var group: String
    private lateinit var email: String

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFutureForTraffic?.isDone == false) return

        status.postValue(Status.STARTED)

        this.uuid = uuid
        this.group = group
        this.email = email

        context.registerReceiver(receiver, intentFilter)

        scheduledFutureForTraffic =  Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                collectTraffic(uuid, group, email)
                collectInstalledApp(uuid, group, email)
            } catch (e: Exception) {
                if (e is SecurityException) {
                    stopCollection()
                    status.postValue(Status.ABORTED(e))
                }
            }
        }, 0, 15, TimeUnit.SECONDS)
    }

    override fun stopCollection() {
        context.unregisterReceiver(receiver)
        scheduledFutureForTraffic?.cancel(true)
        prevRxBytes = null
        prevTxBytes = null
        status.postValue(Status.CANCELED)
    }

    private fun collectInstalledApp(uuid: String, group: String, email: String) {
        val box = App.boxFor<InstalledAppEntity>()
        val now = System.currentTimeMillis()
        val lastTime = box.query().orderDesc(InstalledAppEntity_.timestamp).build().findFirst()?.timestamp ?: Long.MIN_VALUE

        if(lastTime < 0 || now - lastTime >= PERIOD_INSTALLED_APP_IN_MS) {
            val entities = context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA).map {
                InstalledAppEntity(
                    name = Utils.getApplicationName(context, it.packageName) ?: "",
                    packageName = it.packageName,
                    isSystemApp = Utils.isSystemApp(context, it.packageName),
                    isUpdatedSystemApp = Utils.isUpdatedSystemApp(context, it.packageName),
                    firstInstallTime = it.firstInstallTime,
                    lastUpdateTime = it.lastUpdateTime
                ).apply {
                    timestamp = now
                    utcOffset = Utils.utcOffsetInHour()
                    subjectEmail = email
                    experimentUuid = uuid
                    experimentGroup = group
                    isUploaded = false
                }
            }
            box.put(entities)

            entities.forEach{

                /* InstalledApp - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }
        }
    }

    private fun collectTraffic(uuid: String, group: String, email: String) {
        status.postValue(Status.RUNNING)

        if(currentScreenState == DeviceEventType.SCREEN_OFF) {
            prevRxBytes = null
            prevTxBytes = null
            return
        }

        val curRxBytes = TrafficStats.getTotalRxBytes()
        val curTxBytes = TrafficStats.getTotalTxBytes()
        val diffRxBytes = (curRxBytes - (prevRxBytes ?: curRxBytes)) / 1000
        val diffTxBytes = (curTxBytes - (prevTxBytes ?: curTxBytes)) / 1000

        if (prevRxBytes != null && prevTxBytes != null) {
            val it = DataTrafficEntity(
                    duration = PERIOD_DATA_TRAFFIC_IN_MS,
                    rxKiloBytes = diffRxBytes,
                    txKiloBytes = diffTxBytes
            ).apply {
                timestamp = System.currentTimeMillis()
                utcOffset = Utils.utcOffsetInHour()
                subjectEmail = email
                experimentUuid = uuid
                experimentGroup = group
                isUploaded = false
            }
            App.boxFor<DataTrafficEntity>().put(it)

            /* Data Traffic - SW EDIT */
            //val gson = GsonBuilder().setPrettyPrinting().create()
            //val jsonEntity: String = gson.toJson(it)
            /*
            val values = ContentValues()
            values.put(DAO.LOG_FIELD_JSON, jsonEntity)
            val c = context.contentResolver
            val handler = object: AsyncQueryHandler(c) {}
            handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
            */
            //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
        }
        prevRxBytes = curRxBytes
        prevTxBytes = curTxBytes
    }

    private fun extractConnectivityEntity(intent: Intent): ConnectivityEntity? {
        if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) {
            return null
        }

        return ConnectivityEntity(
            type = ConnectivityNetworkType.fromValue(
                intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0),
                ConnectivityNetworkType.UNDEFINED),
            isConnected = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
        ).apply {
            timestamp = System.currentTimeMillis()
            utcOffset = Utils.utcOffsetInHour()
            subjectEmail = email
            experimentUuid = uuid
            experimentGroup = group
            isUploaded = false
        }
    }

    private fun extractBatteryEntity(intent: Intent): BatteryEntity? {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) {
            return null
        }
        return BatteryEntity(
            level = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0).toFloat() * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)),
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0),
            plugged = BatteryPluggedType.fromValue(
                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0), BatteryPluggedType.UNDEFINED
            ),
            status = BatteryStatusType.fromValue(
                intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0), BatteryStatusType.UNDEFINED
            )
        ).apply {
            timestamp = System.currentTimeMillis()
            utcOffset = Utils.utcOffsetInHour()
            subjectEmail = email
            experimentUuid = uuid
            experimentGroup = group
            isUploaded = false
        }
    }

    private fun extractDeviceEventEntity(context: Context?, intent: Intent): DeviceEventEntity? {
        return handleDeviceEvent(context, intent).let {
            if (it != DeviceEventType.UNDEFINED) {
                return@let DeviceEventEntity(
                    type = it
                ).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    subjectEmail = email
                    experimentUuid = uuid
                    experimentGroup = group
                    isUploaded = false
                }
            } else {
                return@let null
            }
        }
    }

    private fun handleDeviceEvent(context: Context?, intent: Intent): DeviceEventType {
        return when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val isPlugged = intent.getIntExtra("step1State", 0) == 1
                val hasMicrophone = intent.getIntExtra("microphone", 0) == 1

                if (isPlugged && hasMicrophone) {
                    DeviceEventType.HEADSET_MIC_PLUGGED
                } else if (!isPlugged && hasMicrophone) {
                    DeviceEventType.HEADSET_MIC_UNPLUGGED
                } else if (isPlugged && !hasMicrophone) {
                    DeviceEventType.HEADSET_PLUGGED
                } else {
                    DeviceEventType.HEADSET_UNPLUGGED
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                DeviceEventType.POWER_CONNECTED
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                DeviceEventType.POWER_DISCONNECTED
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                DeviceEventType.TURN_ON_DEVICE
            }
            Intent.ACTION_SHUTDOWN -> {
                DeviceEventType.TURN_OFF_DEVICE
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                (context?.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.let {
                    return@let if (it.isPowerSaveMode) {
                        DeviceEventType.ACTIVATE_POWER_SAVE_MODE
                    } else {
                        DeviceEventType.DEACTIVATE_POWER_SAVE_MODE
                    }
                } ?:  DeviceEventType.CHANGE_POWER_SAVE_MODE

            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                if (intent.getBooleanExtra("step1State", false)) {
                    DeviceEventType.ACTIVATE_AIRPLANE_MODE
                } else {
                    DeviceEventType.DEACTIVATE_AIRPLANE_MODE
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                DeviceEventType.UNLOCK
            }
            Intent.ACTION_SCREEN_ON -> {
                DeviceEventType.SCREEN_ON
            }
            Intent.ACTION_SCREEN_OFF -> {
                DeviceEventType.SCREEN_OFF
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, 0)) {
                    AudioManager.RINGER_MODE_NORMAL -> {
                        DeviceEventType.RINGER_MODE_NORMAL
                    }
                    AudioManager.RINGER_MODE_SILENT -> {
                        DeviceEventType.RINGER_MODE_SILENT
                    }
                    AudioManager.RINGER_MODE_VIBRATE -> {
                        DeviceEventType.RINGER_MODE_VIBRATE
                    }
                    else -> {
                        DeviceEventType.UNDEFINED
                    }
                }
            }
            Intent.ACTION_BATTERY_OKAY -> {
                DeviceEventType.BATTERY_OKAY
            }
            Intent.ACTION_BATTERY_LOW -> {
                DeviceEventType.BATTERY_LOW
            }
            else -> {
                DeviceEventType.UNDEFINED
            }
        }
    }

    companion object {
        private const val PERIOD_DATA_TRAFFIC_IN_MS : Long = 1000 * 15
        private const val PERIOD_INSTALLED_APP_IN_MS : Long = 1000 * 60 * 60 * 3
        val ACTION_DEVICE_EVENT_CHANGE = "${DeviceEventAndTrafficCollector::class.java.canonicalName}.ACTION_DEVICE_EVENT_CHANGE"
        val EXTRA_DEVICE_EVENT = "${DeviceEventAndTrafficCollector::class.java.canonicalName}.EXTRA_DEVICE_EVENT"

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }

        private val TAG : String = DeviceEventAndTrafficCollector::class.java.simpleName

        fun checkEnableToCollect(context: Context) = true
    }
}