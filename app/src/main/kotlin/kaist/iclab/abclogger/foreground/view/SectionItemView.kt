package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kaist.iclab.abclogger.R

open class SectionItemView(context: Context, attributeSet: AttributeSet?) : FrameLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val sectionHeaderTextView: TextView
    private val container: FrameLayout
    private val relativeLayout: RelativeLayout
    private val btnMore: ImageButton

    init {
        super.setPadding(
            resources.getDimensionPixelOffset(R.dimen.cardViewOuterPadding),
            0,
            resources.getDimensionPixelOffset(R.dimen.cardViewOuterPadding),
            0
        )

        val cardView = androidx.cardview.widget.CardView(context).apply {
            cardElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0F, resources.displayMetrics)
            useCompatPadding = true
        }

        relativeLayout = RelativeLayout(context)
        container = FrameLayout(context).apply {
            id = View.generateViewId()
        }

        sectionHeaderTextView = TextView(context, null, 0, R.style.ABCSubSectionHeader).apply {
            id = View.generateViewId()
            setTextColor(ContextCompat.getColor(context, R.color.colorTitle))
            setTypeface(null, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeTitle))
        }

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)

        btnMore = ImageButton(context).apply {
            id = View.generateViewId()
            setColorFilter(ContextCompat.getColor(context, R.color.colorMessage))
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_more_horiz_black_24))
            if(typedValue.resourceId != 0) {
                setBackgroundResource(typedValue.resourceId)
            } else {
                setBackgroundColor(typedValue.data)
            }
            setPadding(0, 0, 0, 0)
            isClickable = true
            isFocusable = true
        }

        if(attributeSet != null) {
            val array = context.obtainStyledAttributes(attributeSet, R.styleable.SectionItemView)
            val header = array.getString(R.styleable.SectionItemView_headerText)
            val showBottomSpace = array.getBoolean(R.styleable.SectionItemView_showBottomSpace, false)
            val showMore = array.getBoolean(R.styleable.SectionItemView_showMore, false)

            sectionHeaderTextView.visibility = View.VISIBLE
            sectionHeaderTextView.text = header
            btnMore.visibility = if(showMore) View.VISIBLE else View.GONE
            relativeLayout.setPadding(0, 0, 0, if(showBottomSpace) resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical) else 0)

            array.recycle()
        } else {
            sectionHeaderTextView.visibility = View.GONE
            btnMore.visibility = View.GONE
        }

        relativeLayout.addView(
            sectionHeaderTextView,
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(
                    resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                    resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical),
                    if(btnMore.visibility == View.VISIBLE) resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal) else resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                    resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical)
                )
                addRule(RelativeLayout.ALIGN_PARENT_START, sectionHeaderTextView.id)
                addRule(RelativeLayout.ALIGN_PARENT_TOP, sectionHeaderTextView.id)
                addRule(RelativeLayout.START_OF, btnMore.id)
            }
        )

        relativeLayout.addView(
            btnMore,
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal), 0)
                addRule(RelativeLayout.ALIGN_PARENT_END, btnMore.id)
                addRule(RelativeLayout.ALIGN_TOP, sectionHeaderTextView.id)
            }
        )

        relativeLayout.addView(
            container,
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START, container.id)
                addRule(RelativeLayout.BELOW, sectionHeaderTextView.id)
            }
        )
        cardView.addView(relativeLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        super.addView(cardView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
    }

    fun setHeader(text: String?) {
        if(!TextUtils.isEmpty(text)) {
            sectionHeaderTextView.visibility = View.VISIBLE
            sectionHeaderTextView.text = text
        } else {
            sectionHeaderTextView.visibility = View.GONE
        }
    }

    fun setContentView(view: View) {
        container.removeAllViews()
        container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
    }

    fun setShowBottomSpace(show: Boolean) {
        relativeLayout.setPadding(0, 0, 0, if(show) resources.getDimensionPixelOffset(R.dimen.itemSpaceVertical) else 0)
    }

    fun setMoreClickListener (listener: (() -> Unit)? ) {
        btnMore.setOnClickListener { listener?.invoke() }
    }
}