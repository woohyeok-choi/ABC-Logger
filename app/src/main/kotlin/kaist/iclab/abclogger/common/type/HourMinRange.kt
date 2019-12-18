package kaist.iclab.abclogger.common.type

data class HourMinRange(val from: HourMin, val to: HourMin) {
    companion object {
        fun fromString(str: String) : HourMinRange {
            return HourMinRange(
                HourMin.fromString(str.split(" - ")[0]),
                HourMin.fromString(str.split(" - ")[1])
            )
        }
    }

    override fun toString(): String {
        return "$from - $to"
    }

    fun isInRange(now: HourMin) : Boolean {
        return now in from..to
    }

    fun isValid() : Boolean {
        return from < to
    }
}