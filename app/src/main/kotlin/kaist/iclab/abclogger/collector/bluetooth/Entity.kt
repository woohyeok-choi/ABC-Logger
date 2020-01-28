package kaist.iclab.abclogger.collector.bluetooth

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class BluetoothEntity(
        var deviceName: String = "",
        var address: String = "",
        var rssi: Int = Int.MIN_VALUE
) : Base()