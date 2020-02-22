package kaist.iclab.abclogger.collector.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.provider.CallLog
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

class CallLogCollector(private val context: Context) : BaseCollector<CallLogCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessed: Long? = null) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_call_log)

    override val description: String = context.getString(R.string.data_desc_call_log)

    override val requiredPermissions: List<String> = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
    )

    override val newIntentForSetUp: Intent? = null

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        context.contentResolver.safeUnregisterContentObserver(callLogObserver)
        context.contentResolver.safeRegisterContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)
    }

    override suspend fun onStop() {
        context.contentResolver.safeUnregisterContentObserver(callLogObserver)
    }

    private val handler : Handler
        get() {
            val thread = HandlerThread(this::class.java.name)
            thread.start()
            return Handler(thread.looper)
        }

    private val callLogObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleCallLogRetrieval()
        }
    }

    private fun handleCallLogRetrieval() = launch {
        val curTime = System.currentTimeMillis()
        val timestamps = mutableListOf<Long>()
        val lastTimeAccessed = getStatus()?.lastTimeAccessed
                ?: curTime - TimeUnit.DAYS.toMillis(1)

        getRecentContents(
                contentResolver = context.contentResolver,
                uri = CallLog.Calls.CONTENT_URI,
                timeColumn = CallLog.Calls.DATE,
                columns = arrayOf(
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.NUMBER_PRESENTATION,
                        CallLog.Calls.DATA_USAGE
                ),
                lastTime = lastTimeAccessed
        ) { cursor ->
            val timestamp = cursor.getLongOrNull(0) ?: 0
            val number = cursor.getStringOrNull(2) ?: ""

            timestamps.add(timestamp)

            CallLogEntity(
                    duration = cursor.getLongOrNull(1) ?: 0,
                    number = toHash(number, 0, 4),
                    type = callTypeToString(cursor.getIntOrNull(3)),
                    presentation = callPresentationTypeToString(cursor.getIntOrNull(4)),
                    dataUsage = cursor.getLongOrNull(5) ?: 0
            ).fillContact(
                    number = number, contentResolver = context.contentResolver
            ).fill(toMillis(timestamp = timestamp))
        }?.also { entity ->
            ObjBox.put(entity)
            setStatus(Status(lastTime = curTime))
        }

        timestamps.max()?.also { timestamp -> setStatus(Status(lastTimeAccessed = timestamp)) }
    }

    private fun callTypeToString(typeInt: Int?): String = when (typeInt) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE -> "MISSED"
        CallLog.Calls.VOICEMAIL_TYPE -> "VOICE_MAIL"
        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "ANSWERED_EXTERNALLY"
        else -> "UNDEFINED"
    }

    private fun callPresentationTypeToString(typeInt: Int?): String = when (typeInt) {
        CallLog.Calls.PRESENTATION_ALLOWED -> "ALLOWED"
        CallLog.Calls.PRESENTATION_PAYPHONE -> "PAYPHONE"
        CallLog.Calls.PRESENTATION_RESTRICTED -> "RESTRICTED"
        CallLog.Calls.PRESENTATION_UNKNOWN -> "UNKNOWN"
        else -> "UNDEFINED"
    }
}