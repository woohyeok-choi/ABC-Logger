package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a single result of periodic WiFi scanning - typically, scanning returns multiple results.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property name device name 1
 * @property deviceName device name 2
 * @property address device mac address
 * @property serviceUuids service uuid of ble device
 * @property serviceData service data of ble device
 * @property manufacturerSpecificData manufacturer data
 * @property rssi signal strength
 * @property txPower tx power if Android API >=26
 * @property txPowerLevel tx power of ble device
 */

@Entity
data class BluetoothDeviceEntity (
        var name: String = "",
        var deviceName: String = "",
        var address: String = "",
        var serviceUuids: String = "",
        var serviceData: String = "",
        var manufacturerSpecificData: String = "",
        var rssi: Int = Int.MIN_VALUE,
        var txPower: Int = Int.MIN_VALUE,
        var txPowerLevel: Int = Int.MIN_VALUE
) : BaseEntity()