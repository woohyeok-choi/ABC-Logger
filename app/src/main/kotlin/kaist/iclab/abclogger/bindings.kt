package kaist.iclab.abclogger

import android.graphics.Typeface
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import com.google.android.material.textfield.TextInputEditText
import java.lang.Exception

@BindingAdapter("error")
fun setError(view: TextView, error: Exception?) {
    if (error == null) {
        view.text = ""
    } else {
        view.setText(if(error is ABCException) error.stringRes else R.string.error_general)
    }
}

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