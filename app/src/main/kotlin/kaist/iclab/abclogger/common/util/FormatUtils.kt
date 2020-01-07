package kaist.iclab.abclogger.common.util

import android.content.Context
import android.os.Build
import com.google.android.material.textfield.TextInputLayout
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Patterns
import kaist.iclab.abclogger.R
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {
    private val logTimeFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.US)

    fun countNumDigits (num: Long): Int {
        return when (num) {
            in Long.MIN_VALUE until 0 -> -1
            0L -> 1
            else -> (Math.log10(num.toDouble()) + 1).toInt()
        }
    }

    fun formatLogTime(timestamp: Long) : String? {
        return if(timestamp > 0) logTimeFormat.format(GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = timestamp }.time) else null
    }

    fun formatTimeRange(context: Context, startInMillis: Long, endInMillis: Long) : String {
        return DateUtils.formatDateRange(context, startInMillis, endInMillis,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NO_YEAR)
    }

    fun formatHash(input: String, start: Int = 0, end: Int = input.length, algorithm: String = "MD5") : String{
        val safeStart = Math.max(0, start)
        val safeEnd = Math.min(input.length, end)

        val subString = input.substring(safeEnd, input.length - 1).toByteArray()
        val bytes = MessageDigest.getInstance(algorithm).digest(subString)
        println(input.substring(0, if(safeStart < 1) 0 else safeStart - 1))

        return input.substring(safeStart, safeEnd) + "\$" +
            bytes.joinToString(separator = "", transform = {
                it.toInt().and(0xFF).toString(16).padStart(2, '0')
            })
    }

    fun formatTimeBefore(fromTime: Long, toTime: Long): String? {
        return if(fromTime <= 0 || toTime - fromTime >= DateUtils.DAY_IN_MILLIS * 2) {
            null
        } else {
            DateUtils.getRelativeTimeSpanString(fromTime, toTime, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        }
    }

    fun formatTimeBeforeExact(fromTime: Long, toTime: Long): String {
        return DateUtils.getRelativeTimeSpanString(fromTime, toTime, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    }

    fun formatDurationInHour (context: Context, durationInHour: Long): String {
        return when(durationInHour) {
                in 0 until 24 -> " $durationInHour ${context.getString(R.string.general_hour)}"
                in 24 until 24 * 7 -> " ${durationInHour / 24} ${context.getString(R.string.general_day)}"
                else -> " ${durationInHour / 24 / 7 } ${context.getString(R.string.general_week)}"
        }
    }

    fun formatSameDay(context: Context, then: Long, now: Long) : String {
        val thenCal = Calendar.getInstance().apply {
            timeInMillis = then
        }
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = now
        }
        return if(thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
            thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)) {
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_TIME)
        } else if (thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)){
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_DATE)
        } else {
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_YEAR)
        }
    }


    fun formatSameYear(context: Context, then: Long, now: Long) : String {
        val thenCal = Calendar.getInstance().apply {
            timeInMillis = then
        }
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = now
        }
        return if(thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
             DateUtils.formatDateTime(context, then, DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        } else {
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME)
        }
    }

    fun formatDateOnly(context: Context, then: Long, now: Long) : String {
        val thenCal = Calendar.getInstance().apply {
            timeInMillis = then
        }
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = now
        }
        return if(thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_SHOW_DATE)
        } else {
            DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)
        }
    }

    fun formatProductName(): String {
        return "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
    }

    fun validateEmail(text: String?): Boolean {
        return text?.let { Patterns.EMAIL_ADDRESS.matcher(it).matches() } ?: false
    }

    fun validateEmail(text: TextInputLayout?): Boolean {
        return validateEmail(text?.editText?.text?.toString())
    }

    fun validateUrl(text: String?) : Boolean {
        return text?.let { Patterns.WEB_URL.matcher(text).matches() } ?: false
    }

    fun validateUrl(text: TextInputLayout?) : Boolean {
        return validateUrl(text?.editText?.text?.toString())
    }

    fun validateTextLength(text: String?, minLength: Int): Boolean {
        return text?.let { it.length >= minLength } ?: false
    }

    fun validateTextLength(text: TextInputLayout?, minLength: Int): Boolean {
        return validateTextLength(text?.editText?.text?.toString(), minLength)
    }

    fun validateNonEmpty(text: String?): Boolean {
        return text?.let { !TextUtils.isEmpty(it) } ?: false
    }

    fun validateNonEmpty(text: TextInputLayout?) : Boolean {
        return validateNonEmpty(text?.editText?.text?.toString())
    }

    fun validatePhoneNumber(text: String?): Boolean {
        return text?.let {
            Patterns.PHONE.matcher(it).matches()
        } ?: false
    }

    fun validatePhoneNumber(text: TextInputLayout?) : Boolean {
        return validatePhoneNumber(text?.editText?.text?.toString())
    }


}
