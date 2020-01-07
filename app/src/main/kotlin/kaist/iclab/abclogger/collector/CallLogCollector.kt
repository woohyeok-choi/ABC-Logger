package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector

class CallLogCollector(val context: Context) : BaseCollector {
    private val callLogObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                val timestamps = mutableListOf<Long>()

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
                        lastTime = SharedPrefs.lastAccessTimeCallLog
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    val number = cursor.getString(2)

                    timestamps.add(timestamp)

                    CallLogEntity(
                            duration = cursor.getLong(1),
                            number = toHash(number, 0, 4),
                            type = callTypeToString(cursor.getInt(3)),
                            presentation = callPresentationTypeToString(cursor.getInt(4)),
                            dataUsage = cursor.getLong(5)
                    ).fillContact(
                            number = number, contentResolver = context.contentResolver
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeCallLog = timestamps.max() ?: -1
            }
        }
    }

    private fun callTypeToString(typeInt: Int): String = when (typeInt) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE -> "MISSED"
        CallLog.Calls.VOICEMAIL_TYPE -> "VOICE_MAIL"
        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "ANSWERED_EXTERNALLY"
        else -> ""
    }

    private fun callPresentationTypeToString(typeInt: Int): String = when (typeInt) {
        CallLog.Calls.PRESENTATION_ALLOWED -> "ALLOWED"
        CallLog.Calls.PRESENTATION_PAYPHONE -> "PAYPHONE"
        CallLog.Calls.PRESENTATION_RESTRICTED -> "RESTRICTED"
        CallLog.Calls.PRESENTATION_UNKNOWN -> "UNKNOWN"
        else -> ""
    }

    override fun start() {
        context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)
    }

    override fun stop() {
        context.contentResolver.unregisterContentObserver(callLogObserver)
    }

    override fun checkAvailability(): Boolean = Utils.checkPermissionAtRuntime(context, requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
        )

    override val newIntentForSetUp: Intent?
        get() = null

    override val nameRes: Int?
        get() = R.string.data_name_call_log

    override val descriptionRes: Int?
        get() = R.string.data_desc_call_log
}