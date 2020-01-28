package kaist.iclab.abclogger.collector.survey

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Transient
import kaist.iclab.abclogger.collector.Base
import java.util.concurrent.TimeUnit

@Entity
data class SurveyEntity(
        var title: String = "",
        var message: String = "",
        var timeoutPolicy: String = "",
        var timeoutSec: Long = 0,
        var deliveredTime: Long = 0,
        var reactionTime: Long = 0,
        var responseTime: Long = 0,
        var json: String = "",
        @Transient var isResponded: Boolean = responseTime > 0
) : Base() {
    fun isAnswered() : Boolean = responseTime > 0

    fun isExpired() : Boolean = timeoutPolicy == Survey.TIMEOUT_DISABLED &&
            System.currentTimeMillis() > deliveredTime + TimeUnit.SECONDS.toMillis(timeoutSec)

    fun isAvailable() : Boolean = !isAnswered() && !isExpired()

    fun showAltText() : Boolean = timeoutPolicy == Survey.TIMEOUT_ALT_TEXT &&
            System.currentTimeMillis() > deliveredTime + TimeUnit.SECONDS.toMillis(timeoutSec)
}