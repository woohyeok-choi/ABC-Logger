package kaist.iclab.abclogger.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.children
import android.widget.RadioGroup as AndroidRadioGroup

class RadioGroup(context: Context, attrs: AttributeSet?) : AndroidRadioGroup(context, attrs) {
    constructor(context: Context): this(context, null)

    override fun addView(child: View?) {
        child?.isEnabled = isEnabled
        super.addView(child)
    }

    override fun setEnabled(enabled: Boolean) {
        children.forEach { it.isEnabled = isEnabled }
        super.setEnabled(enabled)
    }
}