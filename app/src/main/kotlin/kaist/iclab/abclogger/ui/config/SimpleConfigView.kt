package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.databinding.BindingAdapter
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.setHorizontalPadding
import kaist.iclab.abclogger.setVerticalPadding

class SimpleConfigView (context: Context, attrs: AttributeSet?, styleRes: Int) : ConstraintLayout(context, attrs, styleRes) {

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context): this(context, null, 0)

    private val headerTextView: TextView
    private val descriptionTextView: TextView

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

        addView(headerTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(descriptionTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().apply {
            clone(this@SimpleConfigView)
            connect(headerTextView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(headerTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

            connect(descriptionTextView.id, ConstraintSet.TOP, headerTextView.id, ConstraintSet.BOTTOM)
            connect(descriptionTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        }.applyTo(this)

        setOnClickListener { onClick?.invoke(this) }
    }

    var onClick: ((view: SimpleConfigView) -> Unit)? = null

    var header : String
        get() = headerTextView.text.toString()
        set(value) {
            headerTextView.visibility = if(value.isBlank()) View.GONE else View.VISIBLE
            headerTextView.text = value
        }

    var description : String
        get() = descriptionTextView.text.toString()
        set(value) {
            descriptionTextView.visibility = if(value.isBlank()) View.GONE else View.VISIBLE
            descriptionTextView.text = value
        }
}

@BindingAdapter("header")
fun setSimpleConfigViewHeader(view: SimpleConfigView, header: String?) {
    view.header = header ?: ""
}

@BindingAdapter("description")
fun setSimpleConfigDescription(view: SimpleConfigView, description: String?) {
    view.description = description ?: ""
}