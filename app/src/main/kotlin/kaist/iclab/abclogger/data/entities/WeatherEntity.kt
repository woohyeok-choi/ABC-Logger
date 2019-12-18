package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class WeatherEntity (
    var latitude: Double = Double.MIN_VALUE,
    var longitude: Double = Double.MIN_VALUE,
    var temperature: Float = Float.MIN_VALUE,
    var rainfall: Float = Float.MIN_VALUE,
    var sky: String = "",
    var windEw: Float = Float.MIN_VALUE,
    var windNs: Float = Float.MIN_VALUE,
    var humidity: Float = Float.MIN_VALUE,
    var rainType: String = "",
    var lightning: String = "",
    var windSpeed: Float = Float.MIN_VALUE,
    var windDirection: Float = Float.MIN_VALUE,
    var so2Value: Float = Float.MIN_VALUE,
    var so2Grade: Float = Float.MIN_VALUE,
    var coValue: Float = Float.MIN_VALUE,
    var coGrade: Float = Float.MIN_VALUE,
    var no2Value: Float = Float.MIN_VALUE,
    var no2Grade: Float = Float.MIN_VALUE,
    var o3Value: Float = Float.MIN_VALUE,
    var o3Grade: Float = Float.MIN_VALUE,
    var pm10Value: Float = Float.MIN_VALUE,
    var pm10Grade: Float = Float.MIN_VALUE,
    var pm25Value: Float = Float.MIN_VALUE,
    var pm25Grade: Float = Float.MIN_VALUE,
    var airValue: Float = Float.MIN_VALUE,
    var airGrade: Float = Float.MIN_VALUE
) : BaseEntity()