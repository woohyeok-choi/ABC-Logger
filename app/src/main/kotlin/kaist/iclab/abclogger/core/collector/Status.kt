package kaist.iclab.abclogger.core.collector

import androidx.annotation.StringRes
import java.io.Serializable

sealed class Status

object On: Status()
object Off: Status()
class Error(val error: Throwable): Status()

data class Description(
    @StringRes
    val stringRes: Int,
    val value: Any?
) : Serializable
