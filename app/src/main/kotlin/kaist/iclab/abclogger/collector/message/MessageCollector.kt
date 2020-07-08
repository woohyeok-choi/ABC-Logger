package kaist.iclab.abclogger.collector.message

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Telephony
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.safeRegisterContentObserver
import kaist.iclab.abclogger.commons.safeUnregisterContentObserver
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class MessageCollector(private val context: Context) : BaseCollector<MessageCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessedSms: Long? = null,
                      val lastTimeAccessedMms: Long? = null) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_message)

    override val description: String = context.getString(R.string.data_desc_message)

    override val requiredPermissions: List<String> = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS
    )

    override val newIntentForSetUp: Intent? = null

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        context.contentResolver.safeUnregisterContentObserver(smsObserver)
        context.contentResolver.safeUnregisterContentObserver(mmsObserver)

        context.contentResolver.safeRegisterContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver)
        context.contentResolver.safeRegisterContentObserver(Telephony.Mms.CONTENT_URI, true, mmsObserver)
    }

    override suspend fun onStop() {
        context.contentResolver.safeUnregisterContentObserver(smsObserver)
        context.contentResolver.safeUnregisterContentObserver(mmsObserver)
    }

    private fun handleSmsRetrieval() = launch {
        val curTime = System.currentTimeMillis()
        val timestamps = mutableListOf<Long>()
        val lastTimeAccessed = getStatus()?.lastTimeAccessedSms
                ?: curTime-TimeUnit.DAYS.toMillis(1)

        getRecentContents(
                contentResolver = context.contentResolver,
                uri = Telephony.Sms.CONTENT_URI,
                lastTime = lastTimeAccessed,
                timeColumn = Telephony.Sms.DATE,
                columns = arrayOf(
                        Telephony.Sms.DATE,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.TYPE)
        ) { cursor ->
            val timestamp = cursor.getLongOrNull(0) ?: 0
            val number = cursor.getStringOrNull(1) ?: ""

            timestamps.add(timestamp)

            MessageEntity(
                    number = toHash(number, 0, 4),
                    messageClass = "SMS",
                    messageBox = messageBoxToString(cursor.getIntOrNull(2))
            ).fillContact(
                    number = number,
                    contentResolver = context.contentResolver
            ).fill(toMillis(timestamp = timestamp))
        }?.also { entity ->
            ObjBox.put(entity)
            setStatus(Status(lastTime = curTime))
        }

        timestamps.max()?.also { timestamp -> setStatus(Status(lastTimeAccessedSms = timestamp)) }
    }

    private fun handleMmsRetrieval() = launch {
        val curTime = System.currentTimeMillis()
        val timestamps = mutableListOf<Long>()
        val lastTimeAccessed = getStatus()?.lastTimeAccessedMms
                ?: curTime-TimeUnit.DAYS.toMillis(1)

        getRecentContents(
                contentResolver = context.contentResolver,
                uri = Telephony.Mms.CONTENT_URI,
                lastTime = lastTimeAccessed,
                timeColumn = Telephony.Mms.DATE,
                columns = arrayOf(
                        Telephony.Mms.DATE,
                        Telephony.Mms._ID,
                        Telephony.Mms.MESSAGE_BOX)
        ) { cursor ->
            val timestamp = cursor.getLongOrNull(0) ?: 0
            val number = getMmsAddress(cursor.getLongOrNull(1), context.contentResolver)
                    ?: ""

            timestamps.add(timestamp)

            MessageEntity(
                    number = toHash(number, 0, 4),
                    messageClass = "MMS",
                    messageBox = messageBoxToString(cursor.getIntOrNull(2))
            ).fillContact(
                    number = number,
                    contentResolver = context.contentResolver
            ).fill(toMillis(timestamp = timestamp))
        }?.run {
            ObjBox.put(this)
            setStatus(Status(lastTime = curTime))
        }

        timestamps.max()?.also { timestamp -> setStatus(Status(lastTimeAccessedMms = timestamp)) }
    }

    private val handler : Handler
        get() {
            val thread = HandlerThread(this::class.java.name)
            thread.start()
            return Handler(thread.looper)
        }

    private val smsObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleSmsRetrieval()
        }
    }

    private val mmsObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMmsRetrieval()
        }
    }

    private fun getMmsAddress(id: Long?, contentResolver: ContentResolver): String? {
        if (id == null) return null
        return contentResolver.query(
                Uri.withAppendedPath(
                        Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id.toString()), "addr"),
                arrayOf(Telephony.Mms.Addr.ADDRESS),
                null, null, null
        )?.use {
            return@use if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun messageBoxToString(typeInt: Int?): String = when (typeInt) {
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT -> "DRAFT"
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED -> "FAILED"
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX -> "INBOX"
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX -> "OUTBOX"
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED -> "QUEUED"
        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT -> "SENT"
        else -> "UNDEFINED"
    }
}