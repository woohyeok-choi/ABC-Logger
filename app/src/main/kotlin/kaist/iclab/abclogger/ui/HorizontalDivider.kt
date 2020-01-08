package kaist.iclab.abclogger.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import kaist.iclab.abclogger.R

class HorizontalDivider (context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.color_gray))
    }
}