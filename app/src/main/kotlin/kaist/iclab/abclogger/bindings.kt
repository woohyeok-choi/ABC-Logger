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

@BindingAdapter("status", "showToast")
fun handleContentProgress(view: ContentLoadingProgressBar, status: Status?, showToast: Boolean?) {
    val state = status?.state
    val error = status?.error

    if (state == Status.STATE_LOADING) view.show() else view.hide()
    if (state == Status.STATE_FAILURE && error != null && showToast == true) view.context.showToast(error)
}

@BindingAdapter("status", "showToast")
fun handleSwipeRefresh(view: SwipeRefreshLayout, status: Status?, showToast: Boolean?) {
    val state = status?.state
    val error = status?.error

    view.isRefreshing = state == Status.STATE_LOADING

    if (state == Status.STATE_FAILURE && error != null && showToast == true) view.context.showToast(error)
}