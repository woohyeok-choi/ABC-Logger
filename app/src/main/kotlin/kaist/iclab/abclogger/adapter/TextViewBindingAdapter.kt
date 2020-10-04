package kaist.iclab.abclogger.adapter

import androidx.databinding.BindingAdapter
import com.google.android.material.textview.MaterialTextView
import kaist.iclab.abclogger.commons.getColorFromAttr
import kaist.iclab.abclogger.view.StatusColor

object TextViewBindingAdapter {
    @BindingAdapter("textSet", "index")
    @JvmStatic
    fun setTextSet(view: MaterialTextView, textSet: Set<String>?, index: Int?) {
        if (textSet == null || index == null) return
        view.text = textSet.elementAtOrNull(index)
    }

    @BindingAdapter("statusColor")
    @JvmStatic
    fun setStatus(view: MaterialTextView, statusColor: StatusColor?) {
        if (statusColor == null) return
        val color = getColorFromAttr(view.context, statusColor.attr) ?: return
        view.setTextColor(color)
    }
}