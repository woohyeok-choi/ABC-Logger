package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.databinding.BindingAdapter
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.setHorizontalPadding
import kaist.iclab.abclogger.setVerticalPadding

class SimpleSwitchConfigView (context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val headerTextView: TextView
    private val descriptionTextView: TextView
    private val switch : Switch

    private var onClick: ((view: SimpleSwitchConfigView, checked: Boolean) -> Unit)? = null

    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        if(typedValue.resourceId != 0) {
            setBackgroundResource(typedValue.resourceId)
        } else {
            setBackgroundColor(typedValue.data)
        }

        setHorizontalPadding(resources.getDimensionPixelSize(R.dimen.item_default_horizontal_padding))
        setVerticalPadding(resources.getDimensionPixelSize(R.dimen.item_default_vertical_padding))
        isClickable = true
        isFocusable = true

        headerTextView = TextView(context).apply {
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_title_small))
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        }

        descriptionTextView = TextView(context).apply {
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
            maxLines = 5
        }

        switch = Switch(context).apply {
            id = View.generateViewId()
            showText = false
        }

        addView(headerTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(descriptionTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(switch, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().apply {
            clone(this@SimpleSwitchConfigView)
            connect(headerTextView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(headerTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(headerTextView.id, ConstraintSet.END, switch.id, ConstraintSet.START)

            connect(descriptionTextView.id, ConstraintSet.TOP, headerTextView.id, ConstraintSet.BOTTOM)
            connect(descriptionTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(descriptionTextView.id, ConstraintSet.END, switch.id, ConstraintSet.START)

            connect(switch.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(switch.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(switch.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }.applyTo(this)

        setOnClickListener {
            switch.toggle()
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            onClick?.invoke(this, isChecked)
        }
    }

    var header : String
        get() = headerTextView.text.toString()
        set(value) { headerTextView.text = value }

    var description : String
        get() = descriptionTextView.text.toString()
        set(value) { descriptionTextView.text = value }

    var checked : Boolean
        get() = switch.isChecked
        set(value) { switch.isChecked = value }
}

@BindingAdapter("header", "description", "checked")
fun setDataConfig(view: SimpleSwitchConfigView, header: String?, description: String?, checked: Boolean?) {
    view.header = header ?: ""
    view.description = description ?: ""
    view.checked = checked ?: false
}