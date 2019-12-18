package kaist.iclab.abclogger.background.collector

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.lifecycle.LiveData
import io.objectbox.Box
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.data.entities.BaseEntity
import kaist.iclab.abclogger.data.entities.CallLogEntity
import kaist.iclab.abclogger.data.entities.MessageEntity
import java.util.concurrent.ScheduledFuture

class CallMessageCollector(val context: Context) : BaseCollector {
    fun typeToString(typeInt: Int): String? = when(typeInt) {
        ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "ASSISTANT"
        ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK -> "CALLBACK"
        ContactsContract.CommonDataKinds.Phone.TYPE_CAR -> "CAR"
        ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> "COMPANY_MAIN"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "FAX_HOME"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "FAX_WORK"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
        ContactsContract.CommonDataKinds.Phone.TYPE_ISDN -> "ISDN"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "MAIN"
        ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "MOBILE"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "OTHER"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX -> "OTHER_FAX"
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "PAGER"
        ContactsContract.CommonDataKinds.Phone.TYPE_RADIO -> "RADIO"
        ContactsContract.CommonDataKinds.Phone.TYPE_TELEX -> "TELEX"
        ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> "TTY_TDD"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "WORK_MOBILE"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "WORK_PAGER"
        else -> null
    }

    private fun BaseEntity.applyContact(number: String?, contentResolver: ContentResolver) : BaseEntity {
        if (number == null) return this

        contentResolver.query(
                Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number)),
                arrayOf(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.PINNED
                ),
                null, null, null
        ).use { cursor ->
            if (cursor?.moveToFirst() == true) {
                when(this) {
                    is MessageEntity -> this.apply {
                            contactType = typeToString(cursor.getInt(0)) ?: cursor.getString(1)
                            isStarred = cursor.getInt(2) == 1
                            isPinned = cursor.getInt(3) == 1
                    }
                    is CallLogEntity -> this.apply {
                        contactType = typeToString(cursor.getInt(0)) ?: cursor.getString(1)
                        isStarred = cursor.getInt(2) == 1
                        isPinned = cursor.getInt(3) == 1
                    }
                }
            }
            return this
        }
    }


    private val messageObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = Telephony.Sms.CONTENT_URI,
                        lastTime = SharedPrefs.lastMediaAccessTime,
                        timeColumn = Telephony.Sms.DATE,
                        columns = arrayOf(
                                Telephony.Sms.DATE,
                                Telephony.Sms.ADDRESS,
                                Telephony.Sms.TYPE)
                ) { cursor ->
                    MessageEntity(
                            number = FormatUtils.formatHash(cursor.getString(1), 0, 4),
                            messageClass = "SMS",
                            messageBox = =""
                    )
                }
            }
        }
    }


    override fun start() {
        if (scheduledFuture?.isDone = false) return
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAvailability(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_SMS
    )

    override fun newIntentForSetup(): Intent? {
        return null
    }

    override fun getLiveStatus(): LiveData<Status> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}