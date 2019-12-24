package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.CallLogEntity
import kaist.iclab.abclogger.fillBaseInfo

class CallLogCollector(val context: Context) : BaseCollector {
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
                        lastTime = SharedPrefs.lastCallLogAccessTime
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    val number = cursor.getString(2)

                    timestamps.add(timestamp)

                    CallLogEntity(
                            duration = cursor.getLong(1),
                            number = FormatUtils.formatHash(number, 0, 4),
                            type = callTypeToString(cursor.getInt(3)),
                            presentation = callPresentationTypeToString(cursor.getInt(4)),
                            dataUsage = cursor.getLong(5)
                    ).fillContact(
                            number = number, contentResolver = context.contentResolver
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastCallLogAccessTime = timestamps.max() ?: -1
            }
        }
    }

    override fun start() {
        if(!SharedPrefs.isProvidedCallLog || !checkAvailability()) return

        context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)
    }

    override fun stop() {
        if(!SharedPrefs.isProvidedCallLog || !checkAvailability()) return

        context.contentResolver.unregisterContentObserver(callLogObserver)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
    )

    override fun newIntentForSetup(): Intent? = null
}