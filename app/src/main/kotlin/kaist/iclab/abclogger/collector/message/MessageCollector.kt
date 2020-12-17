package kaist.iclab.abclogger.collector.message

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri

import android.provider.Telephony
import androidx.core.content.getSystemService
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.*
import java.util.concurrent.TimeUnit

class MessageCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<MessageEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS
    )

    override val setupIntent: Intent? = null

    private var lastTimeSmsWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeMmsWritten by ReadWriteStatusLong(Long.MIN_VALUE)

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MESSAGE_SCAN_REQUEST,
            Intent(ACTION_MESSAGE_SCAN_REQUEST),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val alarmManager by lazy {
        context.getSystemService<AlarmManager>()!!
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleMessageScanRequest()
        }
    }

    private val contentResolver by lazy { context.contentResolver }

    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf(
        R.string.collector_message_info_sms_written with
                formatDateTime(context, lastTimeSmsWritten),
        R.string.collector_message_info_mms_written with
                formatDateTime(context, lastTimeMmsWritten)

    )

    override suspend fun onStart() {
        val filter = IntentFilter().apply {
            addAction(ACTION_MESSAGE_SCAN_REQUEST)
        }
        context.safeRegisterReceiver(receiver, filter)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20),
            TimeUnit.MINUTES.toMillis(30),
            intent
        )

    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<MessageEntity>()

    override suspend fun flush(entities: Collection<MessageEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<MessageEntity> =
        dataRepository.find(0, limit)

    private fun handleMessageScanRequest() = launch {
        val toTime = System.currentTimeMillis()
        val fromTimeSms = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeSmsWritten
        )
        val fromTimeMms = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeMmsWritten
        )
        /**
         * Retrieve SMS Messages
         */
        val sms = getRecentContents(
            contentResolver = contentResolver,
            uri = Telephony.Sms.CONTENT_URI,
            lastTimeInMillis = fromTimeSms,
            timeColumn = Telephony.Sms.DATE,
            columns = arrayOf(
                Telephony.Sms.DATE,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.TYPE
            )
        ) { millis, cursor ->
            val number = cursor.getStringOrNull(1) ?: ""
            val contact = getContact(contentResolver, number) ?: Contact()

            MessageEntity(
                number = toHash(number, 4),
                messageClass = "SMS",
                messageBox = stringifyMessageType(cursor.getIntOrNull(2)),
                contactType = contact.contactType,
                isStarred = contact.isStarred,
                isPinned = contact.isPinned
            ).apply {
                timestamp = millis
            }
        }

        sms.forEach { put(it) }

        lastTimeSmsWritten = sms.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeSmsWritten) ?: lastTimeSmsWritten

        /**
         * Retrieve MMS Messages
         */
        val mms = getRecentContentsWithSeconds(
            contentResolver = contentResolver,
            uri = Telephony.Mms.CONTENT_URI,
            /**
             * Time column of MMS messages are stored as second, not millis.
             */
            lastTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(fromTimeMms),
            timeColumn = Telephony.Mms.DATE,
            columns = arrayOf(
                Telephony.Mms.DATE,
                Telephony.Mms._ID,
                Telephony.Mms.MESSAGE_BOX
            )
        ) { millis, cursor ->
            val number = getMmsAddress(contentResolver, cursor.getLongOrNull(1)) ?: ""
            val contact = getContact(contentResolver, number) ?: Contact()

            MessageEntity(
                number = toHash(number, 0),
                messageClass = "MMS",
                messageBox = stringifyMessageType(cursor.getIntOrNull(2)),
                contactType = contact.contactType,
                isStarred = contact.isStarred,
                isPinned = contact.isPinned
            ).apply {
                timestamp = millis
            }
        }

        mms.forEach { put(it) }

        lastTimeMmsWritten = mms.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeMmsWritten) ?: lastTimeMmsWritten
    }

    private fun getMmsAddress(contentResolver: ContentResolver, id: Long?): String? {
        if (id == null) return null
        return contentResolver.query(
            Uri.withAppendedPath(
                Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id.toString()), "addr"
            ),
            arrayOf(Telephony.Mms.Addr.ADDRESS),
            null, null, null
        )?.use {
            return@use if (it.moveToFirst()) it.getString(0) else null
        }
    }

    companion object {
        private const val ACTION_MESSAGE_SCAN_REQUEST =
            "${BuildConfig.APPLICATION_ID}.ACTION_MESSAGE_SCAN_REQUEST"
        private const val REQUEST_CODE_MESSAGE_SCAN_REQUEST = 0x13
    }
}