package kaist.iclab.abclogger.data

import android.content.Context
import android.content.SharedPreferences

class Prefs (context: Context) {
    val PREFS_FILENAME = "kaist.iclab.abclogger.data.prefs"
    val PARTICIPANT_SIGNED_IN = "participant_signed_in"
    val PARTICIPANT_SIGNED_UP = "participant_signed_up"
    val PARTICIPANT_EMAIL = "participant_email"
    val PARTICIPANT_PHONE_NUMBER = "participant_phone_number"
    val PARTICIPANT_GROUP = "participant_group"
    val REQUIRES_EVENT_AND_TRAFFIC = "require_event_and_traffic"
    val REQUIRES_LOCATION_AND_ACTIVITY = "require_location_and_activity"
    val REQUIRES_AMBIANT_SOUND = "requires_ambient_sound"
    val REQUIRES_CONTENT_PROVIDERS = "requires_content_providers"
    val REQUIRES_APP_USAGE = "requires_app_usage"
    val REQUIRES_NOTIFICATION = "requires_notification"
    val REQUIRES_GOOGLE_FITNESS = "requires_google_fitness"
    val LAST_TIME_EXPORT_DB = "last_time_export_db"
    val LAST_TIME_SURVEY_TRIGGERED = "lat_time_survey_triggered"
    val preferences: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var requiresEventAndTraffic: Boolean
        get() = preferences.getBoolean(REQUIRES_EVENT_AND_TRAFFIC, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_EVENT_AND_TRAFFIC, value).apply()

    var requiresLocationAndActivity: Boolean
        get() = preferences.getBoolean(REQUIRES_LOCATION_AND_ACTIVITY, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_LOCATION_AND_ACTIVITY, value).apply()

    var requiresAmbientSound: Boolean
        get() = preferences.getBoolean(REQUIRES_AMBIANT_SOUND, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_AMBIANT_SOUND, value).apply()

    var requiresContentProviders: Boolean
        get() = preferences.getBoolean(REQUIRES_CONTENT_PROVIDERS, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_CONTENT_PROVIDERS, value).apply()

    var requiresAppUsage: Boolean
        get() = preferences.getBoolean(REQUIRES_APP_USAGE, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_APP_USAGE, value).apply()

    var requiresNotification: Boolean
        get() = preferences.getBoolean(REQUIRES_NOTIFICATION, true)
        set(value) = preferences.edit().putBoolean(REQUIRES_NOTIFICATION, value).apply()

    var requiresGoogleFitness: Boolean
        get() = preferences.getBoolean(REQUIRES_GOOGLE_FITNESS, false)
        set(value) = preferences.edit().putBoolean(REQUIRES_GOOGLE_FITNESS, value).apply()

    var participantSignedIn: Boolean
        get() = preferences.getBoolean(PARTICIPANT_SIGNED_IN, false)
        set(value) = preferences.edit().putBoolean(PARTICIPANT_SIGNED_IN, value).apply()

    var participantSignedUp: Boolean
        get() = preferences.getBoolean(PARTICIPANT_SIGNED_UP, false)
        set(value) = preferences.edit().putBoolean(PARTICIPANT_SIGNED_UP, value).apply()

    var participantEmail: String?
        get() = preferences.getString(PARTICIPANT_EMAIL, "sk@k.k")
        set(value) = preferences.edit().putString(PARTICIPANT_EMAIL, value).apply()

    var participantPhoneNumber: String?
        get() = preferences.getString(PARTICIPANT_PHONE_NUMBER, "01012345678")
        set(value) = preferences.edit().putString(PARTICIPANT_PHONE_NUMBER, value).apply()

    var participantGroup: String?
        get() = preferences.getString(PARTICIPANT_GROUP, "suggestbot")
        set(value) = preferences.edit().putString(PARTICIPANT_GROUP, value).apply()

    var lastTimeExportDB: Long
        get() = preferences.getLong(LAST_TIME_EXPORT_DB, 0L)
        set(value) = preferences.edit().putLong(LAST_TIME_EXPORT_DB, value).apply()

    var lastTimeSurveyTriggered: Long
        get() = preferences.getLong(LAST_TIME_SURVEY_TRIGGERED, Long.MIN_VALUE)
        set(value) = preferences.edit().putLong(LAST_TIME_SURVEY_TRIGGERED, value).apply()

}