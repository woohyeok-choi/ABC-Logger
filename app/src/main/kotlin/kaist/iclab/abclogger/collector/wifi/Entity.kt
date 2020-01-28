package kaist.iclab.abclogger.collector.wifi

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.Base

@Entity
data class WifiEntity(
        var bssid: String = "",
        var ssid: String = "",
        var frequency: Int = Int.MIN_VALUE,
        var rssi: Int = Int.MIN_VALUE
) : Base()
