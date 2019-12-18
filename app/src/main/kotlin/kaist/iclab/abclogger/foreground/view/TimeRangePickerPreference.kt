package kaist.iclab.abclogger.foreground.view

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.preference.DialogPreference
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.base.BasePreferenceDialogFragmentCompat
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.type.HourMinRange

class TimeRangePickerPreference: DialogPreference {
    companion object {
        const val DEFAULT_VALUE = "10:00 - 22:00"
    }

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private var timeRange: HourMinRange? = null

    private fun persistTimeRange(formatTime: String) {
        persistTimeRange(HourMinRange.fromString(formatTime))
    }

    fun persistTimeRange(range: HourMinRange) {
        timeRange = range
        summary =  range.toString()
        persistString(range.toString())
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.dialog_time_range_picker
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if(restorePersistedValue) {
            persistTimeRange(
                getPersistedString(DEFAULT_VALUE)
            )
        } else {
            (defaultValue as? String)?.let {
                persistTimeRange(it)
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a?.getString(index) ?: DEFAULT_VALUE
    }

    class PreferenceDialogFragment: BasePreferenceDialogFragmentCompat(), TabLayout.OnTabSelectedListener {
        companion object {
            fun newInstance(key: String): PreferenceDialogFragment {
                return PreferenceDialogFragment().apply {
                    arguments = Bundle(1).apply {
                        putString(ARG_KEY, key)
                    }
                }
            }
        }

        private lateinit var pickerStartTime: TimePicker
        private lateinit var pickerEndTime: TimePicker
        private lateinit var tabDateRange: TabLayout

        override fun onBindDialogView(view: View?) {
            super.onBindDialogView(view)
            view?.let {
                tabDateRange = it.findViewById(R.id.tabDateRange)
                tabDateRange.addOnTabSelectedListener(this@PreferenceDialogFragment)

                val timeRange = (preference as? TimeRangePickerPreference)?.timeRange ?: HourMinRange.fromString(DEFAULT_VALUE)

                pickerStartTime = it.findViewById(R.id.pickerStartTime)
                pickerEndTime = it.findViewById(R.id.pickerEndTime)

                pickerStartTime = pickerStartTime.apply {
                    setIs24HourView(true)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        currentHour = timeRange.from.hour
                        currentMinute = timeRange.from.minute
                    } else {
                        hour = timeRange.from.hour
                        minute = timeRange.from.minute
                    }
                }

                pickerEndTime = pickerEndTime.apply {
                    setIs24HourView(true)
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        currentHour = timeRange.to.hour
                        currentMinute = timeRange.to.minute
                    } else {
                        hour = timeRange.to.hour
                        minute = timeRange.to.minute
                    }
                }
            }
        }

        override fun onDialogClosed(positiveResult: Boolean) {
            if (positiveResult) {
                val timeRange = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    HourMinRange(
                        HourMin(pickerStartTime.currentHour, pickerStartTime.currentMinute),
                        HourMin(pickerEndTime.currentHour, pickerEndTime.currentMinute)
                    )
                } else {
                    HourMinRange(
                        HourMin(pickerStartTime.hour, pickerStartTime.minute),
                        HourMin(pickerEndTime.hour, pickerEndTime.minute)
                    )
                }

                if(!timeRange.isValid()) {
                    Toast.makeText(requireContext(), R.string.error_general_time_range, Toast.LENGTH_SHORT).show()
                    return
                }
                (preference as? TimeRangePickerPreference)?.persistTimeRange(timeRange)
            }
            tabDateRange.removeOnTabSelectedListener(this)
        }

        override fun onTabReselected(tab: TabLayout.Tab?) { }

        override fun onTabUnselected(tab: TabLayout.Tab?) { }

        override fun onTabSelected(tab: TabLayout.Tab?) {
            when(tab?.position) {
                0 -> {
                    pickerStartTime.visibility = View.VISIBLE
                    pickerEndTime.visibility = View.GONE
                }
                1 -> {
                    pickerStartTime.visibility = View.GONE
                    pickerEndTime.visibility = View.VISIBLE
                }
            }
        }
    }
}