package kaist.iclab.abclogger.collector

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.Formatter

fun formatDateTime(context: Context, timeInMillis: Long) =
    timeInMillis.takeIf { it > 0 }?.let { Formatter.formatDateTime(context, timeInMillis) } ?: context.getString(R.string.general_mdash)

internal data class Contact(
        val contactType: String = "UNKNOWN",
        val isStarred: Boolean = false,
        val isPinned: Boolean = false
)

internal suspend fun <R> getRecentContents(
        contentResolver: ContentResolver,
        uri: Uri, timeColumn: String, columns: Array<String>,
        lastTime: Long = -1, block: suspend (cursor: Cursor) -> R): Collection<R> {
    val results = mutableListOf<R>()
    contentResolver.query(
            uri, columns, "$timeColumn > ?", arrayOf(lastTime.toString()), "$timeColumn ASC"
    )?.use { cursor ->
        while (cursor.moveToNext()) results.add(block.invoke(cursor))
    }
    return results
}

internal fun getContact(contentResolver: ContentResolver, number: String?): Contact? {
    if (number.isNullOrBlank()) return null

    return contentResolver.query(
            Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number)),
            arrayOf(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.PINNED
            ),
            null, null, null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null

        Contact(
                contactType = stringifyContactType(cursor.getIntOrNull(0))
                        ?: cursor.getStringOrNull(1) ?: "UNKNOWN",
                isStarred = cursor.getInt(2) == 1,
                isPinned = cursor.getInt(3) == 1
        )
    }
}