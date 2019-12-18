package kaist.iclab.abclogger.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.type.HourMinRange
import kaist.iclab.abclogger.common.type.YearMonthDay
import kaist.iclab.abclogger.foreground.view.TimeRangePickerPreference
import java.util.*
import kotlin.reflect.KProperty

class PreferenceAccessor private constructor(context: Context) {
    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private var instance : PreferenceAccessor? = null

        fun getInstance(context: Context): PreferenceAccessor {
            instance = instance ?: PreferenceAccessor(context)
            return instance!!
        }
    }

    fun clear() {
        isSyncInProgress = false
        isParticipated = false
        nextSurveyTriggeredAt = Long.MIN_VALUE
        lastTimeSynced = Long.MIN_VALUE
        lastTimeSurveyChecked = Long.MIN_VALUE
        lastTimeContentAccessed = Long.MIN_VALUE
        lastTimeSurveyTriggerEventOccurs = Long.MIN_VALUE
        lastTimeSurveyCancelEventOccurs = Long.MIN_VALUE
        lastTimeAppUsageAccessed = Long.MIN_VALUE
        lastTimeGoogleFitnessAccessed = Long.MIN_VALUE
    }

    /**
     * Only a system is allowed to modify values belows (not a user)
     */
    val deviceUuid: String by UuidDelegate(sharedPreferences, "PREF_KEY_DEVICE_UUID", UUID.randomUUID().toString())
    var isSyncInProgress: Boolean by BooleanDelegate(sharedPreferences, "PREF_KEY_IS_SYNC_IN_PROGRESS", false)
    var isParticipated: Boolean by BooleanDelegate(sharedPreferences, "PREF_KEY_IS_PARTICIPATED", false)
    var nextSurveyTriggeredAt: Long by LongDelegate(sharedPreferences, "PREF_KEY_NEXT_SURVEY_TRIGGERED_AT")
    var lastTimeSynced: Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_SYNCED")
    var lastTimeSurveyChecked: Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_SURVEY_CHECKED")
    var lastTimeContentAccessed : Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_CONTENT_ACCESSED")
    var lastTimeSurveyTriggerEventOccurs : Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_SURVEY_TRIGGER_EVENT_OCCURS")
    var lastTimeSurveyCancelEventOccurs : Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_SURVEY_CANCEL_EVENT_OCCURS")
    var lastTimeAppUsageAccessed: Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_APP_USAGE_ACCESSED")
    var lastTimeGoogleFitnessAccessed: Long by LongDelegate(sharedPreferences, "PREF_KEY_LAST_TIME_GOOGLE_FITNESS_ACCESSED")

    class UuidDelegate (private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: String = "") {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): String {
            if(!sharedPreferences.contains(key)) {
                sharedPreferences.edit().putString(key, UUID.randomUUID().toString()).apply()
            }
            return sharedPreferences.getString(key, defaultValue) ?: defaultValue
        }
    }

    class StringDelegate (private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: String = "") {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): String {
            return sharedPreferences.getString(key, defaultValue) ?: defaultValue
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: String) {
            sharedPreferences.edit().putString(key, v).apply()
        }
    }

    class IntDelegate (private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: Int = Int.MIN_VALUE) {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): Int {
            return sharedPreferences.getInt(key, defaultValue)
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: Int) {
            sharedPreferences.edit().putInt(key, v).apply()
        }
    }

    class LongDelegate (private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: Long = Long.MIN_VALUE) {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): Long {
            return sharedPreferences.getLong(key, defaultValue)
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: Long) {
            sharedPreferences.edit().putLong(key, v).apply()
        }
    }

    class BooleanDelegate (private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: Boolean = false) {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): Boolean {
            return sharedPreferences.getBoolean(key, defaultValue)
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: Boolean) {
            sharedPreferences.edit().putBoolean(key, v).apply()
        }
    }

    class YearMonthDayDelegate(private val sharedPreferences: SharedPreferences, val key: String, val defaultValue: YearMonthDay = YearMonthDay.now()) {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): YearMonthDay {
            return YearMonthDay.fromString(sharedPreferences.getString(key, defaultValue.toString())) ?: defaultValue
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: YearMonthDay) {
            sharedPreferences.edit().putString(key, v.toString()).apply()
        }
    }

    class HourMinRangeDelegate (private val sharedPreferences: SharedPreferences, val key: String,
                                val defaultValue: HourMinRange = HourMinRange(HourMin(9, 0), HourMin(21, 0))) {
        operator fun getValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>): HourMinRange {
            return HourMinRange.fromString(sharedPreferences.getString(key, TimeRangePickerPreference.DEFAULT_VALUE) ?: TimeRangePickerPreference.DEFAULT_VALUE)
        }

        operator fun setValue(preferenceAccessor: PreferenceAccessor, property: KProperty<*>, v: HourMinRange) {
            sharedPreferences.edit().putString(key, v.toString()).apply()
        }
    }
}