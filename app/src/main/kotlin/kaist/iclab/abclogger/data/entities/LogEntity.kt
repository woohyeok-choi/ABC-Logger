package kaist.iclab.abclogger.data.entities

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.Utils
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class LogEntity(
    @Id
    var id: Long = 0,
    var timestamp: Long = Long.MIN_VALUE,
    var utcOffset: Float = Float.MIN_VALUE,
    var email: String = "",
    var tag: String = "",
    var message: String = "",
    var isUploaded: Boolean = false
) {
    companion object {
        fun log(tag: String, message: String) {
            Log.d(tag, message)
            App.boxFor<LogEntity>().put(
                LogEntity(
                    timestamp = System.currentTimeMillis(),
                    utcOffset = Utils.utcOffsetInHour(),
                    email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                    tag = tag,
                    message = "${FormatUtils.formatLogTime(System.currentTimeMillis()) ?: "None"} $message"
                )
            )
        }
    }
}