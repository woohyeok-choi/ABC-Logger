package kaist.iclab.abclogger.collector

import android.Manifest
<<<<<<< HEAD
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.base.BaseCollector
import polar.com.sdk.api.PolarBleApi
=======
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.EditText
>>>>>>> master
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarInvalidArgument
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
<<<<<<< HEAD
import java.util.concurrent.TimeUnit

class PolarH10Collector(val context: Context) : BaseCollector {
    private val compositeDisposable = CompositeDisposable()

    private val polarApi: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                        PolarBleApi.FEATURE_DEVICE_INFO or
                        PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).apply {
            setPolarFilter(false)
            setAutomaticReconnection(true)
            setApiCallback(object : PolarBleApiCallback() {
                override fun ecgFeatureReady(identifier: String) {
                    super.ecgFeatureReady(identifier)
                    updateStatus()

                    requestEcgSettings(identifier)
                            .flatMapPublisher { setting -> startEcgStreaming(identifier, setting.maxSettings()) }
                            .buffer(5, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    { data ->
                                        data.map { datum ->
                                            datum.samples.map { ecg ->
                                                SensorEntity(
                                                        sensorId = ExternalDevicePrefs.polarH10DeviceId,
                                                        sensorName = "PolarH10",
                                                        valueType = "ECG",
                                                        valueDescription = "ECG",
                                                        firstValue = ecg.toFloat()
                                                ).fillBaseInfo(
                                                        timeMillis = datum.timeStamp
                                                )
                                            }
                                        }.flatten().run {
                                            putEntity(this)
                                        }
                                    },
                                    { throwable ->
                                        handleExceptionOrDisconnection(throwable)
                                        updateStatus()
                                    }
                            ).let { disposable ->
                                compositeDisposable.add(disposable)
                            }
                }

                override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                    super.deviceConnected(polarDeviceInfo)
                    ExternalDevicePrefs.polarH10Connection = "CONNECTED"
                }

                override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                    super.deviceDisconnected(polarDeviceInfo)
                    handleExceptionOrDisconnection()
                    updateStatus()
                }

                override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                    super.deviceConnecting(polarDeviceInfo)
                    ExternalDevicePrefs.polarH10Connection = "CONNECTING"
                    updateStatus()
                }

                override fun batteryLevelReceived(identifier: String, level: Int) {
                    super.batteryLevelReceived(identifier, level)
                    ExternalDevicePrefs.polarH10BatteryLevel = level
                    updateStatus()
                }

                override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                    val timestamp = System.currentTimeMillis()
                    val heartRate = data.hr
                    val contactStatus = if (data.contactStatus) 1.0F else 0.0F
                    val contactStatusSupported = if (data.contactStatusSupported) 1.0F else 0.0F

                    SensorEntity(
                            sensorId = ExternalDevicePrefs.polarH10DeviceId,
                            sensorName = "PolarH10",
                            valueType = "HeartRate",
                            valueDescription = "HR/ContactStatus/ContactStatusSupported",
                            firstValue = heartRate.toFloat(),
                            secondValue = contactStatus,
                            thirdValue = contactStatusSupported
                    ).fillBaseInfo(timeMillis = timestamp).run { putEntity(this) }

                    if (data.rrAvailable) {
                        data.rrs.zip(data.rrsMs).map { (rrSec, rrMs) ->
                            SensorEntity(
                                    sensorId = identifier,
                                    sensorName = "PolarH10",
                                    valueType = "RRInterval",
                                    valueDescription = "RRsec/RRms/ContactStatus/ContactStatusSupported",
                                    firstValue = rrSec.toFloat(),
                                    secondValue = rrMs.toFloat(),
                                    thirdValue = contactStatus,
                                    fourthValue = contactStatusSupported
                            ).fillBaseInfo(timeMillis = timestamp)
                        }.run { putEntity(this) }
                    }
                }
            })
        }
    }

    private fun handleExceptionOrDisconnection(throwable: Throwable? = null) {
        ExternalDevicePrefs.polarH10Connection = "DISCONNECTED"
        ExternalDevicePrefs.polarH10HrFeatureReady = false
        ExternalDevicePrefs.polarH10EcgFeatureReady = false
        ExternalDevicePrefs.polarH10BatteryLevel = -1
        ExternalDevicePrefs.polarH10Exception = throwable?.message ?: ""
    }

    private fun updateStatus() {
        SharedPrefs.statusPolarH10 = """
                    Connection: ${ExternalDevicePrefs.polarH10Connection}; 
                    HrReady: ${ExternalDevicePrefs.polarH10HrFeatureReady};
                    EcgReady: ${ExternalDevicePrefs.polarH10EcgFeatureReady};
                    Battery: ${ExternalDevicePrefs.polarH10BatteryLevel}
                    Exception: ${ExternalDevicePrefs.polarH10Exception}
                """.trimIndent()
=======

class PolarH10Collector(val context: Context) :
        BaseCollector{

    companion object {
        private val SHARED_PREF_DEVICE_KEY =
                "kaist.iclab.abclogger.collector.polarh10.PolarH10Collector.DEVICE_KEY"

        class SharedPreferenceUtil(context: Context) {
            private val sharedPref: SharedPreferences =
                    context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

            fun getValue(key: String): String? = sharedPref.getString(key, null)

            fun putValue(key: String, value: String) {sharedPref.edit().putString(key, value).commit() }

            fun removeKey(key: String) {sharedPref.edit().remove(key).commit()}

            companion object {
                private var PRIVATE_MODE = 0
                private val PREF_NAME = "kaist.iclab.abclogger.SharedPreferenceUtil"

            }
        }
    }

    private val sharedPrefUtil = SharedPreferenceUtil(context)

    private fun getDeviceId(): String? = sharedPrefUtil.getValue(SHARED_PREF_DEVICE_KEY)

    private fun storeDeviceId(deviceId: String) { sharedPrefUtil.putValue(SHARED_PREF_DEVICE_KEY, deviceId)}

    private fun removeDeviceId(){sharedPrefUtil.removeKey(SHARED_PREF_DEVICE_KEY)}

    fun log(type: String, timestamp: Long, msg: String){
        Log.d(type, "timestamp: $timestamp, msg: $msg") }

    private val api = PolarBleApiDefaultImpl.defaultImplementation(context, 15).apply {
        setPolarFilter(false)
        setAutomaticReconnection(true)

        setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "CONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                log("POLAR BATTERY", System.currentTimeMillis(), "level: $level ")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                log("POLAR HR", System.currentTimeMillis(), "value: ${data.hr}, rrsMs: ${data.rrsMs}, rr: ${data.rrs}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "DISCONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun ecgFeatureReady(identifier: String) {
                requestEcgSettings(identifier).toFlowable()
                        .flatMap {
                            startEcgStreaming(identifier, it.maxSettings())
                        }
                        .subscribe(
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "time (nanasecond): ${it.timeStamp}, values(yV): ${it.samples}")
                                },
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "Error MSG: ${it.toString()}")
                                },
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "complete")
                                }
                        )
            }
        })
>>>>>>> master
    }

    override fun start() {
        try {
<<<<<<< HEAD
            polarApi.connectToDevice(ExternalDevicePrefs.polarH10DeviceId)
        } catch (e: PolarInvalidArgument) {
            handleExceptionOrDisconnection(e)
            updateStatus()
        }
    }

    override fun stop() {
        try {
            polarApi.disconnectFromDevice(ExternalDevicePrefs.polarH10DeviceId)
            compositeDisposable.clear()
        } catch (e: PolarInvalidArgument) {
            handleExceptionOrDisconnection(e)
            updateStatus()
        }
    }

    override fun checkAvailability(): Boolean =
            !TextUtils.isEmpty(ExternalDevicePrefs.polarH10DeviceId) &&
                    Utils.checkPermissionAtRuntime(context, requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PolarH10SettingActivity::class.java)

    override val nameRes: Int?
        get() = R.string.data_name_polar_h10

    override val descriptionRes: Int?
        get() = R.string.data_desc_polar_h10


    class PolarH10SettingActivity : BaseAppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

        }
    }

    companion object {
=======
            api.connectToDevice(getDeviceId()!!)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            removeDeviceId()
            polarInvalidArgument.printStackTrace()
        }
    }

    /**
     * After calling this method - stop(), device ID is no longer stored.
     */
    override fun stop() {
        try {
            api.disconnectFromDevice(getDeviceId()!!)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            polarInvalidArgument.printStackTrace()
        }
        removeDeviceId()
    }

    /**
     * Assuming that permissions are already obtained before calling this method - checkAvailability()
     */
    override fun checkAvailability(): Boolean = (!getDeviceId().isNullOrBlank())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun newIntentForSetup(): Intent? {
        val deviceIdEditText = EditText(context)
        //deviceIdEditText.setText("4373B624")
        AlertDialog.Builder(context)
                .setTitle("Please type Polar H10 ID")
                .setView(deviceIdEditText)
                .setCancelable(false)
                .setPositiveButton("Done") { dialog, id -> storeDeviceId(deviceIdEditText.text.toString()) }
                .create().show()
        return null
>>>>>>> master

    }
}