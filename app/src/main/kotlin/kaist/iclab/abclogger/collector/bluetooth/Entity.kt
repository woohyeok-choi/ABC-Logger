package kaist.iclab.abclogger.collector.bluetooth

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class BluetoothEntity(
        var name: String = "",
        var alias: String = "",
        var address: String = "",
        var bondState: String = "",
        var deviceType: String = "",
        var classType: String = "",
        var rssi: Int = Int.MIN_VALUE,
        var isLowEnergy: Boolean = false
) : AbstractEntity()