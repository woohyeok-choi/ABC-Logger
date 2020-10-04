package kaist.iclab.abclogger.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.view.children

class CheckBoxGroup (context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {
    constructor(context: Context): this(context, null)

    interface OnCheckedChangedListener {
        fun onCheckedChanged(button: CompoundButton, isChecked: Boolean)
    }

    private var onCheckedChangedListener: OnCheckedChangedListener? = null

    fun setOnCheckedChangedListener(listener: OnCheckedChangedListener?) {
        onCheckedChangedListener = listener
    }

    fun setOnCheckedChangedListener(block: (button: CompoundButton, isChecked: Boolean) -> Unit) {
        onCheckedChangedListener = object : OnCheckedChangedListener {
            override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean) {
                block.invoke(button, isChecked)
            }
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child is CheckBox) {
            child.setOnCheckedChangeListener { compoundButton, b ->
                onCheckedChangedListener?.onCheckedChanged(compoundButton, b)
            }
            onCheckedChangedListener?.onCheckedChanged(child, child.isChecked)
        }
        child?.isEnabled = isEnabled
        super.addView(child, index, params)
    }


    override fun setEnabled(enabled: Boolean) {
        children.forEach { it.isEnabled = isEnabled }
        super.setEnabled(enabled)
    }
}