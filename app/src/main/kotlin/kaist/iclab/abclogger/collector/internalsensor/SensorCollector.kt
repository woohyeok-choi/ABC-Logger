package kaist.iclab.abclogger.collector.internalsensor

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SensorCollector(val context: Context) : BaseCollector, SensorEventListener {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val isProximityAvailable: Boolean? = null,
                      val isLightAvailable: Boolean? = null) : BaseStatus() {
        override fun info(): String =
                "Proximity: ${if (isProximityAvailable == true) "On" else "Off"}; Light: ${if (isLightAvailable == true) "On" else "Off"}"
    }

    private val sensorManager: SensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val subject: PublishSubject<SensorEntity> = PublishSubject.create()
    private val disposables: CompositeDisposable = CompositeDisposable()

    private fun accuracyToString(accuracy: Int) = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
        else -> "LOW"
    }

    override suspend fun onStart() {
        disposables.clear()

        sensorManager.unregisterListener(this)

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        lightSensor?.let { sensorManager.registerListener(this, it, TimeUnit.SECONDS.toMicros(3).toInt()) }
        proximitySensor?.let { sensorManager.registerListener(this, it, TimeUnit.SECONDS.toMicros(3).toInt()) }

        setStatus(Status(
                isLightAvailable = lightSensor != null,
                isProximityAvailable = proximitySensor != null)
        )

        val disposable = subject.buffer(
                10, TimeUnit.SECONDS
        ).subscribeOn(
                Schedulers.io()
        ).subscribe { entities ->
            GlobalScope.launch {
                ObjBox.put(entities)
                if (entities.isNotEmpty()) setStatus(Status(lastTime = System.currentTimeMillis()))
            }
        }
        disposables.addAll(disposable)
    }

    override suspend fun onStop() {
        disposables.clear()

        sensorManager.unregisterListener(this)
    }

    override suspend fun checkAvailability(): Boolean = true

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val timestamp = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                SensorEntity(
                        type = "PROXIMITY",
                        accuracy = accuracyToString(event.accuracy),
                        firstValue = event.values?.firstOrNull() ?: Float.MIN_VALUE
                )
            }
            Sensor.TYPE_LIGHT -> {
                SensorEntity(
                        type = "LIGHT",
                        accuracy = accuracyToString(event.accuracy),
                        firstValue = event.values?.firstOrNull() ?: Float.MIN_VALUE
                )
            }
            else -> null
        }?.fill(timeMillis = timestamp)?.let { subject.onNext(it) }
    }
}