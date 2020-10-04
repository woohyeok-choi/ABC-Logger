package kaist.iclab.abclogger.collector.sensor

import android.content.Context
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.AbstractCollector
import kaist.iclab.abclogger.core.DataRepository

abstract class AbstractExternalSensorCollector(
    context: Context,
    name: String,
    qualifiedName: String,
    description: String,
    dataRepository: DataRepository,
    val deviceType: String
) : AbstractCollector<ExternalSensorEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    abstract fun getSensorStatus() : Array<Info>

    override fun getStatus(): Array<Info> = getSensorStatus() + Info(
        stringRes = R.string.collector_info_external_sensor_device_type,
        value = deviceType
    )

    override suspend fun count(): Long = dataRepository.count<ExternalSensorEntity> {
        equal(ExternalSensorEntity_.deviceType, deviceType)
    }

    override suspend fun flush(entities: Collection<ExternalSensorEntity>) {
        dataRepository.remove(entities)
    }

    override suspend fun list(limit: Long): Collection<ExternalSensorEntity> = dataRepository.find(0, limit) {
        equal(ExternalSensorEntity_.deviceType, deviceType)
    }
}