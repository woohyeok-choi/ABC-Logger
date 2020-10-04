package kaist.iclab.abclogger.collector.external

import android.content.Context
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.collector.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import kaist.iclab.abclogger.core.collector.with

abstract class AbstractExternalSensorCollector(
    context: Context,
    qualifiedName: String,
    name: String,
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