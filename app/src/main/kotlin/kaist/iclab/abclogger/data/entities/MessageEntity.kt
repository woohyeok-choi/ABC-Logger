package kaist.iclab.abclogger.data.entities


import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.data.types.*

/**
 * It represents a single item of SMS/MMS logs
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property number a phone number
 * @property messageClass MMS or SMS
 * @property messageBox message box type (e.g., inbox, outbox, draft, etc.)
 * @property contactType a contact type
 * @property isStarred indicates this phone number is favorite or not
 * @property isPinned indicates this phone number is pinned
 */

@Entity
data class MessageEntity (
    var number: String = "",
    var messageClass: String = "",
    var messageBox: String = "",
    var contactType: String = "",
    var isStarred: Boolean = false,
    var isPinned: Boolean = false
) : BaseEntity()