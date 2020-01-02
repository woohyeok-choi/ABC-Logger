package kaist.iclab.abclogger.collector

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.Base
import kaist.iclab.abclogger.CallLogEntity
import kaist.iclab.abclogger.MessageEntity
import kaist.iclab.abclogger.ObjBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun <T> getRecentContents(contentResolver: ContentResolver,
                          uri: Uri, timeColumn: String, columns: Array<String>,
                          lastTime: Long = -1, transform: (cursor: Cursor) -> T): List<T>? {
    return contentResolver.query(
            uri, columns, "$timeColumn > ?", arrayOf(lastTime.toString()), "$timeColumn ASC"
    ).use { cursor ->
        val entities = mutableListOf<T>()
        while (cursor?.moveToNext() == true) {
            entities.add(transform(cursor))
        }
        return@use if (entities.isEmpty()) null else entities
    }
}

fun contactTypeToString(typeInt: Int): String? = when (typeInt) {
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


fun <T : Base> T.fillContact(number: String?, contentResolver: ContentResolver): T {
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
            when (this) {
                is MessageEntity -> this.apply {
                    contactType = contactTypeToString(cursor.getInt(0)) ?: cursor.getString(1)
                    isStarred = cursor.getInt(2) == 1
                    isPinned = cursor.getInt(3) == 1
                }
                is CallLogEntity -> this.apply {
                    contactType = contactTypeToString(cursor.getInt(0)) ?: cursor.getString(1)
                    isStarred = cursor.getInt(2) == 1
                    isPinned = cursor.getInt(3) == 1
                }
            }
        }
        return this
    }
}

fun toMillis(timestamp: Long): Long {
    val curMillis = System.currentTimeMillis()
    return if (FormatUtils.countNumDigits(timestamp) == FormatUtils.countNumDigits(curMillis)) {
        timestamp
    } else {
        timestamp * 1000
    }
}


fun getApplicationName(packageManager: PackageManager, packageName: String): String? =
        try {
            packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.GET_META_DATA
            ).let {
                packageManager.getApplicationLabel(it).toString()
            }
        } catch (e: Exception) {
            null
        }


fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean =
        try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
                it.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
            }
        } catch (e: Exception) {
            false
        }


fun isUpdatedSystemApp(packageManager: PackageManager, packageName: String): Boolean =
        try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
                it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            }
        } catch (e: Exception) {
            false
        }


inline fun <reified T: Base> putEntity(entity: T?) {
    entity?.let { e ->
        GlobalScope.launch(Dispatchers.IO) {
            ObjBox.boxFor<T>().put(e)
        }
    }
}

inline fun <reified T: Base> putEntity(entities: Collection<T>?) {
    if (entities?.isNotEmpty() == true) {
        GlobalScope.launch(Dispatchers.IO) {
            ObjBox.boxFor<T>().put(entities)
        }
    }
}
