package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a single result of periodic WiFi scanning - typically, scanning returns multiple results.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property bssid bssid of this AP
 * @property ssid ssid of this AP
 * @property frequency frequency of this AP
 * @property rssi signal strength
 */

@Entity
data class WifiEntity (
    var bssid: String = "",
    var ssid: String = "",
    var frequency: Int = Int.MIN_VALUE,
    var rssi: Int = Int.MIN_VALUE
) : BaseEntity()