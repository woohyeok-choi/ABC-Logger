package kaist.iclab.abclogger.ui.settings.polar

data class Connection(
    val name: String,
    val address: String,
    val rssi: Int,
    val state: String
)

data class HeartRate(
        val heartRate: Int,
        val rrInterval: List<Int>,
        val contact: Boolean
)
