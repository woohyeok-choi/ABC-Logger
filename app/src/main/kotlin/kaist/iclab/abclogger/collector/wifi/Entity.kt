package kaist.iclab.abclogger.collector.wifi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.JsonConverter

@Entity
data class WifiEntity(
        @Convert(converter = WifiConverter::class, dbType = String::class)
        var accessPoints: List<AccessPoint> = listOf()
) : AbstractEntity() {
    data class AccessPoint(
            var bssid: String = "",
            var ssid: String = "",
            var frequency: Int = Int.MIN_VALUE,
            var rssi: Int = Int.MIN_VALUE
    )
}

class WifiConverter : JsonConverter<List<WifiEntity.AccessPoint>>(
        adapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Types.newParameterizedType(List::class.java, WifiEntity.AccessPoint::class.java)),
        default = listOf()
)
