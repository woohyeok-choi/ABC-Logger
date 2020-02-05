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

@BindingAdapter("timestamp")
fun setFormattedTime(view: TextView, timestamp: Long?) {
    timestamp?.let { then ->
        view.text = formatSameDayTimeYear(view.context, then, System.currentTimeMillis())
    }
}

@BindingAdapter("status")
fun handleContentProgress(view: ContentLoadingProgressBar, status: Status?) {
    val state = status?.state

    if (state == Status.STATE_LOADING) view.show() else view.hide()
}

@BindingAdapter("showToast")
fun showToast(view: View, showToast: Status?) {
    val state = showToast?.state
    val error = showToast?.error

    if (state == Status.STATE_FAILURE && error != null) view.context.showToast(error)
}
