package kaist.iclab.abclogger.view

import androidx.annotation.AttrRes
import kaist.iclab.abclogger.R

enum class StatusColor (@AttrRes val attr: Int) {
    NONE(R.attr.colorOnBackground),
    NORMAL(R.attr.colorPrimary),
    ERROR(R.attr.colorError)
}