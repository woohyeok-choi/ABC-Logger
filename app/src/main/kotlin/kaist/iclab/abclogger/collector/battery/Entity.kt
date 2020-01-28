package kaist.iclab.abclogger.collector.battery

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class BatteryEntity(
        var level: Int = Int.MIN_VALUE,
        var scale: Int = Int.MIN_VALUE,
        var temperature: Int = Int.MIN_VALUE,
        var voltage: Int = Int.MIN_VALUE,
        var health: String = "",
        var pluggedType: String = "",
        var status: String = ""
) : Base()
