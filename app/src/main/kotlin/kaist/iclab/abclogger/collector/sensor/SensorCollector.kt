package kaist.iclab.abclogger.collector.sensor

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.fill
import java.util.concurrent.TimeUnit

class SensorCollector(val context: Context) : BaseCollector, SensorEventListener {
    private val sensorManager : SensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val subject : PublishSubject<SensorEntity> = PublishSubject.create<SensorEntity>()
    private val disposables: CompositeDisposable = CompositeDisposable()

    private fun accuracyToString(accuracy: Int) = when(accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
        else -> "LOW"
    }

    override suspend fun onStart() {
        disposables.clear()
        sensorManager.unregisterListener(this)

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { lightSensor ->
            sensorManager.registerListener(this, lightSensor, TimeUnit.SECONDS.toMicros(1).toInt())
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { proxyMeter ->
            sensorManager.registerListener(this, proxyMeter, TimeUnit.SECONDS.toMicros(1).toInt())
        }

        val disposable = subject.buffer(
                10, TimeUnit.SECONDS
        ).subscribeOn(
                Schedulers.io()
        ).subscribe { entities ->
            ObjBox.put(entities)
        }

        disposables.addAll(disposable)
    }

    override suspend fun onStop() {
        disposables.clear()

        sensorManager.unregisterListener(this)
    }

    override fun checkAvailability(): Boolean = true

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onSensorChanged(event: SensorEvent?) {
        val timestamp = System.currentTimeMillis()
        event?.let { e ->
            val entity = when(e.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    SensorEntity(
                            type = "PROXIMITY",
                            accuracy = accuracyToString(e.accuracy),
                            firstValue = e.values?.firstOrNull() ?: Float.MIN_VALUE
                    )
                }
                Sensor.TYPE_LIGHT -> {
                    SensorEntity(
                            type = "LIGHT",
                            accuracy = accuracyToString(e.accuracy),
                            firstValue = e.values?.firstOrNull() ?: Float.MIN_VALUE
                    )
                }
                else -> null
            }?.fill(timeMillis = timestamp) ?: return@let
            subject.onNext(entity)
        }
    }
}