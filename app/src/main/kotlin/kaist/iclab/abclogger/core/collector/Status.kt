package kaist.iclab.abclogger.core.collector

import androidx.annotation.StringRes
import java.io.Serializable

sealed class Status {
    object On: Status()
    object Off: Status()
    class Error(val message: String?): Status()
}

data class Description(
    @StringRes
    val stringRes: Int,
    val value: Any?
) : Serializable

infix fun Int.with (value: Any?) = Description(this, value)

