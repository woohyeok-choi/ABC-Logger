package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import kaist.iclab.abclogger.R

class SurveyItemView(context: Context, attributeSet: AttributeSet?) : RelativeLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val txtTitle = TextView(context).apply {
        id = View.generateViewId()
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 2
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeTitle))
    }

    private val txtMessage: TextView = TextView(context).apply {
        id = View.generateViewId()
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 2
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeMessage))
    }

    private val txtDeliveredTime: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
    }

    init {
        setPadding(
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
            resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical)
        )

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        if(typedValue.resourceId != 0) {
            setBackgroundResource(typedValue.resourceId)
        } else {
            setBackgroundColor(typedValue.data)
        }
        isClickable = true
        isFocusable = true

        addView(txtTitle, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, txtTitle.id)
            addRule(RelativeLayout.ALIGN_PARENT_TOP, txtTitle.id)
            addRule(RelativeLayout.START_OF, txtDeliveredTime.id)
        })

        addView(txtMessage, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, txtMessage.id)
            addRule(RelativeLayout.BELOW, txtTitle.id)
        })

        addView(txtDeliveredTime, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP, txtDeliveredTime.id)
            addRule(RelativeLayout.ALIGN_PARENT_END, txtDeliveredTime.id)
        })

        setValidItem(true)
    }

    fun getTitleView() : View {
      return txtTitle
    }

    fun getMessageView() : View {
        return txtMessage
    }

    fun getDeliveredTimeView() : View {
        return txtDeliveredTime
    }

    fun clearView() {
        val loadingTxt = context.getString(R.string.general_loading)
        txtTitle.text = loadingTxt
        txtMessage.text = loadingTxt
        txtDeliveredTime.text = loadingTxt
    }

    fun setValidItem(isValid: Boolean) {
        txtTitle.setTypeface(null, if(isValid) Typeface.BOLD else Typeface.NORMAL)
        txtTitle.setTextColor(ContextCompat.getColor(context, if(isValid) R.color.colorTitle else R.color.colorMessage))

        txtMessage.setTypeface(null, if(isValid) Typeface.BOLD else Typeface.NORMAL)
        txtMessage.setTextColor(ContextCompat.getColor(context, R.color.colorMessage))

        txtDeliveredTime.setTypeface(null, if(isValid) Typeface.BOLD else Typeface.NORMAL)
        txtDeliveredTime.setTextColor(ContextCompat.getColor(context, if(isValid) R.color.colorBlue else R.color.colorMessage))
    }

    fun setTitle(text: String?) {
        if(TextUtils.isEmpty(text)) {
            txtTitle.visibility = View.GONE
        } else {
            txtTitle.visibility = View.VISIBLE
            txtTitle.text = text
        }
    }

    fun setMessage(text: String?) {
        if(TextUtils.isEmpty(text)) {
            txtMessage.visibility = View.GONE
        } else {
            txtMessage.visibility = View.VISIBLE
            txtMessage.text = text
        }
    }

    fun setDeliveredTime(text: String) {
        if(TextUtils.isEmpty(text)) {
            txtDeliveredTime.visibility = View.GONE
        } else {
            txtDeliveredTime.visibility = View.VISIBLE
            txtDeliveredTime.text = text
        }
    }
}