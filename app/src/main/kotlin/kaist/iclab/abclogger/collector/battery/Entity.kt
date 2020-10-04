package kaist.iclab.abclogger.collector.battery

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class BatteryEntity(
        var level: Int = Int.MIN_VALUE,
        var scale: Int = Int.MIN_VALUE,
        var temperature: Int = Int.MIN_VALUE,
        var voltage: Int = Int.MIN_VALUE,
        var health: String = "",
        var pluggedType: String = "",
        var status: String = "",
        var capacity: Int = Int.MIN_VALUE,
        var chargeCounter: Int = Int.MIN_VALUE,
        var currentAverage: Int = Int.MIN_VALUE,
        var currentNow: Int = Int.MIN_VALUE,
        var energyCounter: Long = Long.MIN_VALUE,
        var technology: String = ""
) : AbstractEntity()
