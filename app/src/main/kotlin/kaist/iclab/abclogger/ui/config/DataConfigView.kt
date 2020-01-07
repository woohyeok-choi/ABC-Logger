package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.widget.RelativeLayout
import kaist.iclab.abclogger.base.BaseCollector

class DataConfigView<T : BaseCollector> (context: Context, collector: T?) : RelativeLayout(context) {
    constructor(context: Context): this(context, null)

}