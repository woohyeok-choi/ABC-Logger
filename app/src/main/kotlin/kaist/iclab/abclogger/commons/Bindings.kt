package kaist.iclab.abclogger

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kaist.iclab.abclogger.ui.Status

@BindingAdapter("isBold")
fun isBold(view: TextView, isBold: Boolean) {
    view.setTypeface(null, if(isBold) Typeface.BOLD else Typeface.NORMAL)
}

@BindingAdapter("formattedTime")
fun setFormattedTime(view: TextView, formattedTime: Long?) {
    formattedTime?.let { then ->
        view.text = formatSameDayTimeYear(view.context, then, System.currentTimeMillis())
    }
}

@BindingAdapter("isLoading")
fun handleContentProgress(view: ContentLoadingProgressBar, isLoading: Boolean?) {
    if (isLoading == true) view.show() else view.hide()
}

