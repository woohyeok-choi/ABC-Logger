package kaist.iclab.abclogger.collector.externalsensor.polar

import android.Manifest
import android.content.Context
import android.content.Intent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.externalsensor.ExternalSensorEntity
import kaist.iclab.abclogger.collector.externalsensor.polar.setting.PolarH10SettingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PolarH10Collector(val context: Context) : BaseCollector, PolarBleApiCallback() {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val deviceId: String? = null,
                      val connection: String? = null,
                      val batteryLevel: Int? = null) : BaseStatus() {

        override fun info(): String = "Id: ${deviceId ?: "UNKNOWN"}; " +
                "Connected: ${connection ?: "UNKNOWN"}; " +
                "Battery: ${batteryLevel ?: "UNKNOWN"}"
    }

    private val disposables = CompositeDisposable()
    private val subject: PublishSubject<ExternalSensorEntity> = PublishSubject.create()

    private val polarApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
                context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                        PolarBleApi.FEATURE_DEVICE_INFO or
                        PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).apply {
            setPolarFilter(false)
            setAutomaticReconnection(true)
        }
    }

    private fun handleEcgStreaming(identifier: String) =
            polarApi.requestEcgSettings(identifier)
                    .flatMapPublisher { setting ->
                        polarApi.startEcgStreaming(identifier, setting.maxSettings())
                    }.map { data ->
                        data.samples.map { ecg ->
                            ExternalSensorEntity(
                                    sensorId = identifier,
                                    name = "PolarH10",
                                    description = "ECG/mV",
                                    firstValue = ecg.toFloat()
                            ).fill(timeMillis = data.timeStamp)
                        }
                    }.buffer(
                            5, TimeUnit.SECONDS
                    ).observeOn(
                            Schedulers.from(Dispatchers.IO.asExecutor())
                    ).subscribeOn(
                            Schedulers.from(Dispatchers.IO.asExecutor())
                    ).subscribe { entities ->
                        GlobalScope.launch {
                            ObjBox.put(entities.flatten())
                            setStatus(Status(lastTime = System.currentTimeMillis()))
                        }
                    }

    override fun ecgFeatureReady(identifier: String) {
        super.ecgFeatureReady(identifier)
        handleEcgStreaming(identifier)?.let { disposables.add(it) }
    }

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnected(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "CONNECTED")) }
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnecting(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "CONNECTING")) }
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceDisconnected(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "DISCONNECTED")) }
        disposables.clear()
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        super.batteryLevelReceived(identifier, level)
        GlobalScope.launch { setStatus(Status(batteryLevel = level)) }
    }

    override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
        super.hrNotificationReceived(identifier, data)

        val timestamp = System.currentTimeMillis()
        val heartRate = data.hr
        val contactStatus = data.contactStatus
        val contactStatusSupported = data.contactStatusSupported

        ExternalSensorEntity(
                sensorId = identifier,
                name = "PolarH10",
                description = "HR/ContactStatus/ContactStatusSupported",
                firstValue = heartRate.toFloat(),
                secondValue = if (contactStatus) 1.0F else 0.0F,
                thirdValue = if (contactStatusSupported) 1.0F else 0.0F
        ).fill(timeMillis = timestamp).let { subject.onNext(it) }

        if (data.rrAvailable) {
            data.rrs.zip(data.rrsMs).map { (rrSec, rrMs) ->
                ExternalSensorEntity(
                        sensorId = identifier,
                        name = "PolarH10",
                        description = "RRsec/RRms/ContactStatus/ContactStatusSupported",
                        firstValue = rrSec.toFloat(),
                        secondValue = rrMs.toFloat(),
                        thirdValue = if (contactStatus) 1.0F else 0.0F,
                        fourthValue = if (contactStatusSupported) 1.0F else 0.0F
                ).fill(timeMillis = timestamp)
            }.forEach { subject.onNext(it) }
        }
    }

    override suspend fun onStart() {
        disposables.clear()

        polarApi.connectToDevice((getStatus() as? Status)?.deviceId ?: "")
        polarApi.setApiCallback(this)

        val disposable = subject.buffer(
                10, TimeUnit.SECONDS
        ).subscribeOn(
                Schedulers.io()
        ).subscribe { entities ->
            GlobalScope.launch {
                ObjBox.put(entities)
                setStatus(Status(lastTime = System.currentTimeMillis()))
            }
        }

        disposables.add(disposable)
    }

    override suspend fun onStop() {
        disposables.clear()

        try {
            polarApi.disconnectFromDevice((getStatus() as? Status)?.deviceId ?: "")
        } catch (e: Exception) {
        }
        try {
            polarApi.setApiCallback(null)
        } catch (e: Exception) {
        }
    }

    override suspend fun checkAvailability(): Boolean = !(getStatus() as? Status)?.deviceId.isNullOrBlank() && context.checkPermission(requiredPermissions)

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PolarH10SettingActivity::class.java)
}