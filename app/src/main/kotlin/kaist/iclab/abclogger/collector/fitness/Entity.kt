package kaist.iclab.abclogger.collector.fitness

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class FitnessEntity(
        var type: String = "",
        var startTime: Long = 0,
        var endTime: Long = 0,
        var value: String = "",
        var fitnessDeviceModel: String = "",
        var fitnessDeviceManufacturer: String = "",
        var fitnessDeviceType: String = "",
        var dataSourceName: String = "",
        var dataSourcePackageName: String = ""
) : AbstractEntity()
