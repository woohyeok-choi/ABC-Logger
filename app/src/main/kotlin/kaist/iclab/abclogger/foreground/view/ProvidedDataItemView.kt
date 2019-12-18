package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.*
import kaist.iclab.abclogger.R

class ProvidedDataItemView(context: Context, attributeSet: AttributeSet?): RelativeLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val headerTextView: TextView
    private val descriptionTextView: TextView
    private val imgMore: ImageView

    init {
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.ProvidedDataItemView)
        val header = array.getString(R.styleable.ProvidedDataItemView_headerText)
        val description = array.getString(R.styleable.ProvidedDataItemView_descriptionText)
        val showMore = array.getBoolean(R.styleable.ProvidedDataItemView_showMore, false)
        val dividerPosition = array.getInteger(R.styleable.ProvidedDataItemView_dividerPosition, 1)

        array.recycle()

  /*      layoutParams = RelativeLayout.LayoutParams(context, attributeSet).apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.WRAP_CONTENT
        }*/

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        if(typedValue.resourceId != 0) {
            setBackgroundResource(typedValue.resourceId)
        } else {
            setBackgroundColor(typedValue.data)
        }

        isClickable = showMore
        isFocusable = showMore

        val innerLayout = RelativeLayout(context, attributeSet).apply {
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical)
            )
            id = View.generateViewId()
        }

        headerTextView = TextView(context).apply {
            text = header
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeTitle))
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        }

        descriptionTextView = TextView(context).apply {
            text = description
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
            maxLines = 5
        }

        imgMore = ImageView(context).apply {
            id = View.generateViewId()
            setColorFilter(ContextCompat.getColor(context, R.color.colorMessage))
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_more_horiz_black_24))
        }

        innerLayout.addView(headerTextView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP, headerTextView.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, headerTextView.id)
            addRule(RelativeLayout.START_OF, imgMore.id)
        })

        innerLayout.addView(descriptionTextView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START, descriptionTextView.id)
            addRule(RelativeLayout.BELOW, headerTextView.id)
            addRule(RelativeLayout.START_OF, imgMore.id)
        })

        innerLayout.addView(imgMore, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END, imgMore.id)
            addRule(RelativeLayout.ALIGN_TOP, headerTextView.id)
            addRule(RelativeLayout.ALIGN_BOTTOM, descriptionTextView.id)
        })

        if(dividerPosition != -1) {
            val divider = View(context).apply {
                id = View.generateViewId()
                setBackgroundColor(ContextCompat.getColor(context, R.color.colorDisabled))
            }
            addView(divider, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0F, resources.displayMetrics).toInt()).apply {
                addRule(if(dividerPosition == 0) RelativeLayout.ALIGN_TOP else RelativeLayout.ALIGN_BOTTOM, innerLayout.id)
            })
        }
        addView(innerLayout, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT))

        setShowMore(showMore)
    }

    fun setHeader(text: String) {
        headerTextView.text = text
    }

    fun setShowMore(showMore: Boolean) {
        imgMore.visibility = if(showMore) View.VISIBLE else View.GONE
        isClickable = showMore
        isFocusable = showMore
    }

    fun setGranted(isGranted: Boolean) {
        imgMore.setImageDrawable(
            if(isGranted) ContextCompat.getDrawable(context, R.drawable.baseline_check_black_24) else ContextCompat.getDrawable(context, R.drawable.baseline_more_horiz_black_24)
        )
    }

    fun setDescription(text: String?) {
        descriptionTextView.text = text ?: ""
    }
}
