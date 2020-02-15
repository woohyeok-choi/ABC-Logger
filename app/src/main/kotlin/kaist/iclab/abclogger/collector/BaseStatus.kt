package kaist.iclab.abclogger.collector

import java.text.SimpleDateFormat
import java.util.*

abstract class BaseStatus(open val hasStarted: Boolean? = null,
                          open val lastTime: Long? = null) {
    abstract fun info(): Map<String, Any>

    companion object {
        private val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)

        fun <T : BaseStatus> information(status: T?): String {
            val timeStr = status?.lastTime?.let { t ->
                val time = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = t
                }
                timeFormat.format(time.time)
            } ?: "Unknown"

            val info = status?.info() ?: mapOf()
            val allInfo = listOf("Last time: $timeStr") + info.map { (k, v) -> "$k: $v" }
            return allInfo.filter { !it.isBlank() }.joinToString(System.lineSeparator()) { "- $it" }
        }
    }
}