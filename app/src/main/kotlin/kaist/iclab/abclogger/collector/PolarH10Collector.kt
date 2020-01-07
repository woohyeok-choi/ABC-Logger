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
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
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
}