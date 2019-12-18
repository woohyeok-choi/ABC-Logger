package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a user'transitionScanClient media generation, such as taking a picture or a video.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property mimetype media'transitionScanClient mimetype (e.g., image/jpeg, video/mp4)
 * @property bucketDisplay typically, it represents the app that is used to take a picture or a video (e.g., Foodie)
 */
@Entity
data class MediaEntity (
    var mimetype: String = "",
    var bucketDisplay: String = ""
) : BaseEntity()