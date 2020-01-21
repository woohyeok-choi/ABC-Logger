package kaist.iclab.abclogger

import android.graphics.Typeface
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("isBold")
fun isBold(view: TextView, isBold: Boolean) {
    view.setTypeface(null, if(isBold) Typeface.BOLD else Typeface.NORMAL)
}

@BindingAdapter("timestamp")
fun setFormattedTime(view: TextView, timestamp: Long?) {
    timestamp?.let { then ->
        view.text = formatSameDayTimeYear(view.context, then, System.currentTimeMillis())
    }
}