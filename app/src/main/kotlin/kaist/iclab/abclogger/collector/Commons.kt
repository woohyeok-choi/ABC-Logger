package kaist.iclab.abclogger.collector

import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.call.CallLogEntity
import kaist.iclab.abclogger.collector.message.MessageEntity
import java.security.MessageDigest
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

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

fun toHash(input: String, start: Int = 0, end: Int = input.length, algorithm: String = "MD5"): String {
    val safeStart = max(0, start)
    val safeEnd = min(input.length, end)

    val subString = input.substring(safeEnd, input.length - 1).toByteArray()
    val bytes = MessageDigest.getInstance(algorithm).digest(subString)
    println(input.substring(0, if (safeStart < 1) 0 else safeStart - 1))

    return input.substring(safeStart, safeEnd) + "\$" +
            bytes.joinToString(separator = "", transform = {
                it.toInt().and(0xFF).toString(16).padStart(2, '0')
            })
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

fun countNumDigits(num: Long): Int {
    return when (num) {
        in Long.MIN_VALUE until 0 -> -1
        0L -> 1
        else -> (log10(num.toDouble()) + 1).toInt()
    }
}

fun toMillis(timestamp: Long): Long {
    val curMillis = System.currentTimeMillis()
    return if (countNumDigits(timestamp) == countNumDigits(curMillis)) {
        timestamp
    } else {
        timestamp * 1000
    }
}

fun getApplicationName(packageManager: PackageManager, packageName: String?): String? {
    packageName ?: return null

    return try {
        packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
        ).let {
            packageManager.getApplicationLabel(it).toString()
        }
    } catch (e: Exception) {
        null
    }
}


fun isSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
        }
    } catch (e: Exception) {
        false
    }
}


fun isUpdatedSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
    } catch (e: Exception) {
        false
    }
}
