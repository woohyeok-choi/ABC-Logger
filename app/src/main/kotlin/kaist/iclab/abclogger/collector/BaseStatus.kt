package kaist.iclab.abclogger.collector

import java.text.SimpleDateFormat
import java.util.*

abstract class BaseStatus(open val hasStarted: Boolean? = null,
                          open val lastTime: Long? = null,
                          open val lastError: Throwable? = null) {
    abstract fun info() : String

    companion object {
        private val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)

        fun <T : BaseStatus> information(status: T?) : String {
            val timeStr = status?.lastTime?.let { t ->
                val time = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = t
                }
                timeFormat.format(time.time)
            } ?: "Unknown"

            return listOf("Last time: $timeStr", status?.info()).filter { !it.isNullOrBlank() }.joinToString(System.lineSeparator())
        }
    }
}