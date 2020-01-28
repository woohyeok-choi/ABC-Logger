package kaist.iclab.abclogger.collector.traffic

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class DataTrafficEntity(
        var fromTime: Long = Long.MIN_VALUE,
        var rxBytes: Long = Long.MIN_VALUE,
        var txBytes: Long = Long.MIN_VALUE,
        var mobileRxBytes: Long = Long.MIN_VALUE,
        var mobileTxBytes: Long = Long.MIN_VALUE
) : Base()
