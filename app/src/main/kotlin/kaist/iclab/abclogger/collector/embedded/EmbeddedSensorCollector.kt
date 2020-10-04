package kaist.iclab.abclogger.collector.sensor

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.AbstractCollector
import kaist.iclab.abclogger.collector.stringifySensorAccuracy
import kaist.iclab.abclogger.core.DataRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import java.util.concurrent.TimeUnit

class EmbeddedSensorCollector(
    context: Context,
    name: String,
    qualifiedName: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<EmbeddedSensorEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = null

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return
            handleSensorEvent(event, System.currentTimeMillis())
        }
    }

    private val buffer: Subject<EmbeddedSensorEntity> = PublishSubject.create()

    override fun isAvailable(): Boolean =
        isSensorAvailable(Sensor.TYPE_LIGHT) || isSensorAvailable(Sensor.TYPE_PROXIMITY)

    override fun getStatus(): Array<Info> = arrayOf(
        Info(
            R.string.collector_info_embedded_sensor_light_enabled,
            context.getString(if (isSensorAvailable(Sensor.TYPE_LIGHT)) R.string.general_enabled else R.string.general_disabled)
        ),
        Info(
            R.string.collector_info_embedded_sensor_proximity_enabled,
            context.getString(if (isSensorAvailable(Sensor.TYPE_PROXIMITY)) R.string.general_enabled else R.string.general_disabled)
        )
    )

    override suspend fun onStart() {
        sensorManager.unregisterListener(listener)

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor ->
            sensorManager.registerListener(
                listener,
                sensor,
                TimeUnit.SECONDS.toMicros(SAMPLING_PERIOD_IN_SEC.toLong()).toInt()
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
            sensorManager.registerListener(
                listener,
                sensor,
                TimeUnit.SECONDS.toMicros(SAMPLING_PERIOD_IN_SEC.toLong()).toInt()
            )
        }

        launch {
            buffer.buffer(
                10, TimeUnit.SECONDS
            ).toFlowable(
                BackpressureStrategy.BUFFER
            ).asFlow().collect {
                put(data = it)
            }
        }
    }

    override suspend fun onStop() {
        sensorManager.unregisterListener(listener)
    }

    override suspend fun count(): Long = dataRepository.count<EmbeddedSensorEntity>()

    override suspend fun flush(entities: Collection<EmbeddedSensorEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<EmbeddedSensorEntity> =
        dataRepository.find(0, limit)

    private fun handleSensorEvent(event: SensorEvent, timestamp: Long) {
        val type = event.sensor.type
        if (type !in arrayOf(Sensor.TYPE_PROXIMITY, Sensor.TYPE_LIGHT)) return

        val entity = EmbeddedSensorEntity(
            valueType = if (type == Sensor.TYPE_PROXIMITY) "PROXIMITY" else "LIGHT",
            status = mapOf(
                "accuracy" to stringifySensorAccuracy(event.accuracy),
                "samplingPeriodInSec" to SAMPLING_PERIOD_IN_SEC.toString()
            ),
            valueUnit = if (type == Sensor.TYPE_PROXIMITY) "centimetre" else "lux",
            valueFormat = "FLOAT",
            values = listOfNotNull(
                event.values.firstOrNull()?.toString()
            )
        )
        entity.timestamp = timestamp
        buffer.onNext(entity)
    }

    private fun isSensorAvailable(type: Int): Boolean = sensorManager.getDefaultSensor(type) != null

    companion object {
        private const val SAMPLING_PERIOD_IN_SEC = 3
    }
}