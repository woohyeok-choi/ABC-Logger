package kaist.iclab.abclogger.ui.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import kaist.iclab.abclogger.R

class DefaultItemView(context: Context, attributeSet: AttributeSet): RelativeLayout(context, attributeSet) {
    private val headerTextView: TextView
    private val descriptionTextView: TextView
    private val imgMore: ImageView

    init {
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.DefaultItemView)
        val header = array.getString(R.styleable.DefaultItemView_headerText)
        val description = array.getString(R.styleable.DefaultItemView_descriptionText)
        val dividerPosition = array.getInteger(R.styleable.DefaultItemView_dividerPosition, -1)

        array.recycle()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        if(typedValue.resourceId != 0) {
            setBackgroundResource(typedValue.resourceId)
        } else {
            setBackgroundColor(typedValue.data)
        }

        isClickable = false
        isFocusable = false

        layoutParams = RelativeLayout.LayoutParams(context, attributeSet).apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.WRAP_CONTENT
        }

        val innerLayout = RelativeLayout(context, attributeSet).apply {
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.item_space_horizontal),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
                resources.getDimensionPixelOffset(R.dimen.item_space_horizontal),
                resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical)
            )
            id = View.generateViewId()
        }

        headerTextView = TextView(context).apply {
            text = header
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.txt_size_title_small))
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        }

        descriptionTextView = TextView(context).apply {
            text = description
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_small_text))
            maxLines = 5
        }

        imgMore = ImageView(context).apply {
            id = View.generateViewId()
            setColorFilter(ContextCompat.getColor(context, R.color.color_message))
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
                setBackgroundColor(ContextCompat.getColor(context, R.color.color_disabled))
            }
            addView(divider, RelativeLayout.LayoutParams(context, attributeSet).apply {
                width = LayoutParams.MATCH_PARENT
                height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0F, resources.displayMetrics).toInt()
                addRule(if(dividerPosition == 0) RelativeLayout.ALIGN_TOP else RelativeLayout.ALIGN_BOTTOM, innerLayout.id)
            })
        }
        addView(innerLayout, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT))
    }

    fun setShowMore(showMore: Boolean) {
        imgMore.visibility = if(showMore) View.VISIBLE else View.GONE
        isClickable = showMore
        isFocusable = showMore
    }

    fun setHeader(header: String) {
        headerTextView.text = header
    }

    fun setDescription(description: String) {
        descriptionTextView.text = description
    }
}