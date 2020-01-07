package kaist.iclab.abclogger.ui.survey.list

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kaist.iclab.abclogger.R

class SurveyListItemView (context: Context) : RelativeLayout(context) {
    private val txtTitle = TextView(context).apply {
        id = View.generateViewId()
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 2
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_title_small))
    }

    private val txtMessage: TextView = TextView(context).apply {
        id = View.generateViewId()
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 2
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_message))
    }

    private val txtDeliveredTime: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_small_text))
    }

    init {
        setPadding(
                resources.getDimensionPixelOffset(R.dimen.item_space_horizontal),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
                resources.getDimensionPixelOffset(R.dimen.item_space_horizontal),
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

        addView(txtTitle, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(ALIGN_PARENT_START, txtTitle.id)
            addRule(ALIGN_PARENT_TOP, txtTitle.id)
            addRule(START_OF, txtDeliveredTime.id)
        })

        addView(txtMessage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(ALIGN_PARENT_START, txtMessage.id)
            addRule(BELOW, txtTitle.id)
        })

        addView(txtDeliveredTime, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(ALIGN_PARENT_TOP, txtDeliveredTime.id)
            addRule(ALIGN_PARENT_END, txtDeliveredTime.id)
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
        txtTitle.setTextColor(ContextCompat.getColor(context, if(isValid) R.color.color_title else R.color.color_message))

        txtMessage.setTypeface(null, if(isValid) Typeface.BOLD else Typeface.NORMAL)
        txtMessage.setTextColor(ContextCompat.getColor(context, R.color.color_message))

        txtDeliveredTime.setTypeface(null, if(isValid) Typeface.BOLD else Typeface.NORMAL)
        txtDeliveredTime.setTextColor(ContextCompat.getColor(context, if(isValid) R.color.color_blue else R.color.color_message))
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