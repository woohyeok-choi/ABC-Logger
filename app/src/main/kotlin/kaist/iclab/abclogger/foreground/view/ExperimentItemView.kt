package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.util.FormatUtils
//import kaist.iclab.abc.protos.ExperimentProtos
import java.util.*

class ExperimentItemView(context: Context, attributeSet: AttributeSet?) : RelativeLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val txtRecruit: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTypeface(null, Typeface.BOLD)
    }
    private val txtDeadline: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTypeface(null, Typeface.BOLD)
    }
    private val txtTitle: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeTitle))
    }
    private val txtAffiliation: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
        setTypeface(null, Typeface.BOLD)
    }
    private val txtRegisteredTime: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
    }
    private val txtSubjects: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
    }
    private val txtDuration: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
    }
    private val txtCompensation: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
    }
    private val txtDailyTimeRange: TextView = TextView(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
        setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
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

        val dot1 = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
            setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            setTypeface(null, Typeface.BOLD)
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0,
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0
            )
            text = resources.getString(R.string.general_dot)
            gravity = Gravity.CENTER
        }

        val dot2 = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
            setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            setTypeface(null, Typeface.BOLD)
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0,
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0
            )
            text = resources.getString(R.string.general_dot)
            gravity = Gravity.CENTER
        }

        val dot3 = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txtSizeSmallText))
            setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            setTypeface(null, Typeface.BOLD)
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0,
                resources.getDimensionPixelOffset(R.dimen.dotPaddingHorizontal),
                0
            )
            text = resources.getString(R.string.general_dot)
            gravity = Gravity.CENTER
        }

        addView(txtRecruit, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP, txtRecruit.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, txtRecruit.id)
        })
        addView(txtDeadline, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP, txtDeadline.id)
            addRule(RelativeLayout.END_OF, txtRecruit.id)
        })
        addView(txtTitle, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtRecruit.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, txtTitle.id)
        })
        addView(txtAffiliation, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtTitle.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, txtAffiliation.id)
        })
        addView(txtRegisteredTime, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, txtRegisteredTime.id)
        })
        addView(dot1, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, txtRegisteredTime.id)
        })
        addView(txtSubjects, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, dot1.id)
        })
        addView(dot2, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, txtSubjects.id)
        })
        addView(txtDuration, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, dot2.id)
        })
        addView(dot3, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, txtDuration.id)
        })
        addView(txtCompensation, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtAffiliation.id)
            addRule(RelativeLayout.END_OF, dot3.id)
        })
        addView(txtDailyTimeRange, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, txtRegisteredTime.id)
            addRule(RelativeLayout.ALIGN_PARENT_START, txtDailyTimeRange.id)
        })
    }

    fun setIsOpenedExperiment(isOpened: Boolean) {
        if(isOpened) {
            txtRecruit.text = context.getString(R.string.general_experiment_open)
            txtRecruit.setTextColor(ContextCompat.getColor(context, R.color.colorBlue))
            txtDeadline.setTextColor(ContextCompat.getColor(context, R.color.colorBlue))
            txtTitle.setTextColor(ContextCompat.getColor(context, R.color.colorTitle))
            txtTitle.setTypeface(null, Typeface.BOLD)
        } else {
            txtRecruit.text = context.getString(R.string.general_experiment_close)
            txtRecruit.setTextColor(ContextCompat.getColor(context, R.color.colorTitle))
            txtRecruit.setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            txtTitle.setTextColor(ContextCompat.getColor(context, R.color.colorMessage))
            txtTitle.setTypeface(null, Typeface.NORMAL)
        }
    }

    fun clear() {
        val loadingText = context.getString(R.string.general_loading)
        txtRecruit.text = loadingText
        txtDeadline.text = loadingText
        txtTitle.text = loadingText
        txtAffiliation.text = loadingText
        txtRegisteredTime.text = loadingText
        txtSubjects.text = loadingText
        txtDuration.text = loadingText
        txtCompensation.text = loadingText
        txtDailyTimeRange.text = loadingText
    }

    fun setRecruit(text: String) {
        txtRecruit.text = text
    }

    fun setDeadline(text: String) {
        txtDeadline.text = text
    }

    fun setTitle(text: String) {
        txtTitle.text = text
    }

    fun setAffiliation(text: String) {
        txtAffiliation.text = text
    }

    fun setRegisteredTime(text: String) {
        txtRegisteredTime.text = text
    }

    fun setSubjects(text: String) {
        txtSubjects.text = text
    }

    fun setDuration(text: String) {
        txtDuration.text = text
    }

    fun setCompensation(text: String) {
        txtCompensation.text = text
    }

    fun setDailyTimeRange(text: String) {
        txtDailyTimeRange.text = text
    }

    fun bindView(isOpened: Boolean, deadlineTimestamp: Long, title: String, affiliation: String, registeredTimestamp: Long,
                 currentSubjects: Int, maxSubjects: Int, durationInHour: Long, compensation: String,
                 dailyStartTime: HourMin, dailyEndTime: HourMin, containsWeekend: Boolean) {
        val isAvailable = deadlineTimestamp > System.currentTimeMillis() &&
            isOpened &&
            currentSubjects < maxSubjects

        setIsOpenedExperiment(isAvailable)

        val curTime = GregorianCalendar.getInstance(Locale.getDefault()).time
        val fromCal = GregorianCalendar.getInstance(Locale.getDefault()).apply {
            time = curTime
            set(Calendar.HOUR_OF_DAY, dailyStartTime.hour)
            set(Calendar.MINUTE, dailyStartTime.minute)
        }

        val toCal = GregorianCalendar.getInstance(Locale.getDefault()).apply {
            time = curTime
            set(Calendar.HOUR_OF_DAY, dailyEndTime.hour)
            set(Calendar.MINUTE, dailyEndTime.minute)
        }
        setDeadline(" (~ ${FormatUtils.formatDateOnly(context, deadlineTimestamp, System.currentTimeMillis())})")
        setTitle(title)
        setAffiliation(affiliation)
        setRegisteredTime(FormatUtils.formatSameDay(context, registeredTimestamp, System.currentTimeMillis()))
        setSubjects(String.format("%s / %s %s", currentSubjects, maxSubjects, context.getString(R.string.general_people)))
        setDuration(FormatUtils.formatDurationInHour(context, durationInHour))
        setCompensation(compensation)
        setDailyTimeRange(String.format("%s, %s (%s)",
            context.getString(R.string.general_daily),
            FormatUtils.formatTimeRange(context, fromCal.timeInMillis, toCal.timeInMillis),
            if (containsWeekend) context.getString(R.string.general_contains_weekend) else context.getString(R.string.general_not_contains_weekend)
        ))
    }

    fun bindView(basic: UInt?, constraint: UInt?) {
        if (basic == null || constraint == null) {
            clear()
            return
        }

        /*
        val isOpened = basic.deadlineTimestamp > System.currentTimeMillis() &&
            constraint.isOpened &&
            basic.currentSubjects < basic.maxSubjects
        */
        val isOpened = true

        setIsOpenedExperiment(isOpened)

        val curTime = GregorianCalendar.getInstance(Locale.getDefault()).time
        val fromCal = GregorianCalendar.getInstance(Locale.getDefault()).apply {
            time = curTime
            /*
            set(Calendar.HOUR_OF_DAY, constraint.dailyStartTime.hour)
            set(Calendar.MINUTE, constraint.dailyStartTime.min)
            */
        }

        val toCal = GregorianCalendar.getInstance(Locale.getDefault()).apply {
            time = curTime
            /*
            set(Calendar.HOUR_OF_DAY, constraint.dailyEndTime.hour)
            set(Calendar.MINUTE, constraint.dailyEndTime.min)
            */
        }
        /*
        setDeadline(" (~ ${FormatUtils.formatDateOnly(context, basic.deadlineTimestamp, System.currentTimeMillis())})")
        setTitle(basic.title)
        setAffiliation(basic.affiliation)
        setRegisteredTime(FormatUtils.formatSameDay(context, basic.registeredTimestamp, System.currentTimeMillis()))
        setSubjects(String.format("%s / %s %s", basic.currentSubjects, basic.maxSubjects, context.getString(R.string.general_people)))
        setDuration(FormatUtils.formatDurationInHour(context, constraint.durationInHour))
        setCompensation(basic.compensation)
        setDailyTimeRange(String.format("%s, %s (%s)",
            context.getString(R.string.general_daily),
            FormatUtils.formatTimeRange(context, fromCal.timeInMillis, toCal.timeInMillis),
            if (constraint.containsWeekend) context.getString(R.string.general_contains_weekend) else context.getString(R.string.general_not_contains_weekend)
        ))
        */
    }

    fun bindView(experiment: UInt? = null) {
        // bindView(experiment?.basic, experiment?.constraint)
    }
}
