package kaist.iclab.abclogger.collector

import java.text.SimpleDateFormat
import java.util.*

abstract class BaseStatus {
    abstract val hasStarted: Boolean?
    abstract val lastTime: Long?

    fun toInfo() : String {
        val timeStr = lastTime?.let { t ->
            val time = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = t
            }
            timeFormat.format(time.time)
        } ?: "Unknown"
        return listOf("Last time: $timeStr", info()).filter { !it.isBlank() }.joinToString(System.lineSeparator())
    }

    abstract fun info() : String

    companion object {
        private val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
    }
}