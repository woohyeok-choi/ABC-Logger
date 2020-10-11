package kaist.iclab.abclogger.commons

import android.content.Context
import android.text.format.DateUtils
import com.ibm.icu.text.CompactDecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Formatter {
    @JvmStatic
    fun formatDateTime(
            context: Context,
            millis: Long
    ) = DateUtils.formatDateTime(
                context,
                millis,
                DateUtils.FORMAT_SHOW_TIME or
                        DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_SHOW_WEEKDAY or
                        DateUtils.FORMAT_SHOW_YEAR or
                        DateUtils.FORMAT_ABBREV_ALL
        )

    @JvmStatic
    fun formatDate(
            context: Context,
            millis: Long
    ) = DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_DATE
    )


    @JvmStatic
    fun formatSameDateTime(
            context: Context,
            then: Long,
            now: Long = System.currentTimeMillis()
    ): String {
        val thenCal = GregorianCalendar.getInstance().apply {
            timeInMillis = then
        }

        val nowCal = GregorianCalendar.getInstance().apply {
            timeInMillis = now
        }

        val isSameDay = thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)

        val isSameYear = thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

        return when {
            isSameDay -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_TIME)
            isSameYear -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_DATE)
            else -> DateUtils.formatDateTime(context, then, DateUtils.FORMAT_SHOW_YEAR)
        }
    }

    @JvmStatic
    fun formatCompactNumber(number: Long): String =
        CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.LONG).format(number)

    @JvmStatic
    fun formatCompactNumber(number: Int): String =
        CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.LONG).format(number)
}