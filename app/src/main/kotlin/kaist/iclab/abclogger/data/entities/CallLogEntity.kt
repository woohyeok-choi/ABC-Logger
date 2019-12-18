package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.data.types.*

/**
 * It represents a single item in call logs.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property duration a duration of call time
 * @property number a phone number
 * @property type a call type (e.g., MISSED, OUTGOING, INCOMING)
 * @property presentation a presentation type that is shown in a screen when a call incomes.
 * @property dataUsage an amount of data usage when any data-related call (e.g., video call)
 * @property contactType a contact type
 * @property isStarred indicates this phone number is favorite or not
 * @property isPinned indicates this phone number is pinned
 */

@Entity
data class CallLogEntity (
    var duration: Long = Long.MIN_VALUE,
    var number: String = "",
    var type: String = "",
    var presentation: String = "",
    var dataUsage: Long = Long.MIN_VALUE,
    var contactType: String = "",
    var isStarred: Boolean = false,
    var isPinned: Boolean = false
): BaseEntity()
