package kaist.iclab.abclogger.commons

import android.content.Context
import com.github.kittinunf.fuel.core.FuelError
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kaist.iclab.abclogger.R
import polar.com.sdk.api.errors.*

abstract class AbcException(
        override val message: String?
) : Exception(message) {
    constructor() : this(null)

    abstract val stringRes: Int

    fun toString(context: Context): String = listOf(
            context.getString(stringRes), message
    ).filter { !it.isNullOrEmpty() }.joinToString(separator = " - ")

    companion object {
        fun wrap(t: Throwable?) = when (t) {
            is ApiException -> GoogleApiError(t.statusCode)
            is FirebaseAuthInvalidUserException -> FirebaseInvalidUserException()
            is FirebaseAuthInvalidCredentialsException -> FirebaseInvalidCredentialException()
            is FirebaseAuthUserCollisionException -> FirebaseUserCollisionException()
            is FuelError -> HttpRequestException(t.message)
            is PolarDeviceDisconnected -> PolarH10Exception("Device is disconnected.")
            is PolarDeviceNotFound -> PolarH10Exception("Device is not found.")
            is PolarDeviceNotConnected -> PolarH10Exception("Device is not connected.")
            is PolarInvalidArgument -> PolarH10Exception("Device id is invalid.")
            is PolarServiceNotAvailable -> PolarH10Exception("Polar service is not available.")
            is AbcException -> t
            else -> UnhandledException(t?.message)
        }
    }
}

class UnhandledException(message: String?) : AbcException(message) {
    override val stringRes: Int
        get() = R.string.error_general
}

class EmptySurveyException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_empty_survey
}

class InvalidSurveyFormatException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_invalid_survey_format
}

class InvalidEntityIdException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_invalid_entity_id
}

class PermissionDeniedException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_permission_denied
}

class WhiteListDeniedException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_not_white_listed
}

class GoogleApiError(private val statusCode: Int) : AbcException() {
    override val message: String?
        get() = when (statusCode) {
            CommonStatusCodes.API_NOT_CONNECTED -> "API_NOT_CONNECTED: API is not connected."
            CommonStatusCodes.CANCELED -> "CANCELED: Request is canceled."
            CommonStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR: API is incorrectly configured."
            CommonStatusCodes.ERROR -> "ERROR: Unknown error occurs."
            CommonStatusCodes.INTERNAL_ERROR -> "INTERNAL_ERROR: Internal error occurs"
            CommonStatusCodes.INTERRUPTED -> "INTERRUPTED: Request is interrupted."
            CommonStatusCodes.INVALID_ACCOUNT -> "INVALID_ACCOUNT: Invalid account name."
            CommonStatusCodes.NETWORK_ERROR -> "NETWORK_ERROR: Network error occurs."
            CommonStatusCodes.RESOLUTION_REQUIRED -> "RESOLUTION_REQUIRED: Resolution is required."
            CommonStatusCodes.SIGN_IN_REQUIRED -> "SIGN_IN_REQUIRED: Sign-in is required."
            CommonStatusCodes.TIMEOUT -> "TIMEOUT: Request is timed out."
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "SIGN_IN_CANCELLED: Sign-in canceled."
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "SIGN_IN_CURRENTLY_IN_PROGRESS: Sign-in is already in progress."
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "SIGN_IN_FAILED: Sign-in failed. There may be no Google account connected to this device, or Google Play service is outdated."
            FitnessStatusCodes.SUCCESS_NO_DATA_SOURCES -> "SUCCESS_NO_DATA_SOURCES: Subscription success but no data source was available."
            FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED -> "SUCCESS_ALREADY_SUBSCRIBED: Subscription success but it was already subscribed."
            FitnessStatusCodes.SUCCESS_NO_CLAIMED_DEVICE -> "SUCCESS_NO_CLAIMED_DEVICE: Request success but no matching device was found."
            FitnessStatusCodes.SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES -> "SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES: Request success but no registered listeners was found. "
            FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS -> "NEEDS_OAUTH_PERMISSIONS: O-Auth permission is required."
            FitnessStatusCodes.CONFLICTING_DATA_TYPE -> "CONFLICTING_DATA_TYPE: Attempted to insert a conflicting data type"
            FitnessStatusCodes.INCONSISTENT_DATA_TYPE -> "INCONSISTENT_DATA_TYPE: Attempted to insert data type whose name does not match to the package name."
            FitnessStatusCodes.DATA_TYPE_NOT_FOUND -> "DATA_TYPE_NOT_FOUND: Unknown data type was requested"
            FitnessStatusCodes.APP_MISMATCH -> "APP_MISMATCH: Attempted to insert data from the wrong app."
            FitnessStatusCodes.UNKNOWN_AUTH_ERROR -> "UNKNOWN_AUTH_ERROR: Unknown error occurs during authorization"
            FitnessStatusCodes.UNSUPPORTED_PLATFORM -> "UNSUPPORTED_PLATFORM: Not supported by Google Fit."
            FitnessStatusCodes.TRANSIENT_ERROR -> "TRANSIENT_ERROR: Transient errors occurs."
            FitnessStatusCodes.EQUIVALENT_SESSION_ENDED -> "EQUIVALENT_SESSION_ENDED: The session was already ended."
            FitnessStatusCodes.APP_NOT_FIT_ENABLED -> "APP_NOT_FIT_ENABLED: The app was not found in a list of connected apps."
            FitnessStatusCodes.API_EXCEPTION -> "API_EXCEPTION: Error occurs in connecting to Google backend."
            FitnessStatusCodes.AGGREGATION_NOT_SUPPORTED -> "AGGREGATION_NOT_SUPPORTED: Aggregation request is not supported."
            FitnessStatusCodes.UNSUPPORTED_ACCOUNT -> "UNSUPPORTED_ACCOUNT: The account is not supported."
            FitnessStatusCodes.DISABLED_BLUETOOTH -> "DISABLED_BLUETOOTH: Bluetooth is currently disabled."
            FitnessStatusCodes.INCONSISTENT_PACKAGE_NAME -> "INCONSISTENT_PACKAGE_NAME: Attempted to insert data for a data source that does not match to the package name."
            FitnessStatusCodes.DATA_SOURCE_NOT_FOUND -> "DATA_SOURCE_NOT_FOUND: No local data source is available."
            FitnessStatusCodes.DATASET_TIMESTAMP_INCONSISTENT_WITH_UPDATE_TIME_RANGE -> "DATASET_TIMESTAMP_INCONSISTENT_WITH_UPDATE_TIME_RANGE: Attempted to update data with out-bound timestamp."
            FitnessStatusCodes.INVALID_SESSION_TIMESTAMPS -> "INVALID_SESSION_TIMESTAMPS: Attempted to insert a session with invalid time range."
            FitnessStatusCodes.INVALID_DATA_POINT -> "INVALID_DATA_POINT: Attempted to insert invalid data."
            FitnessStatusCodes.INVALID_TIMESTAMP -> "INVALID_TIMESTAMP: Attempted to query data with invalid timestamps."
            FitnessStatusCodes.DATA_TYPE_NOT_ALLOWED_FOR_API -> "DATA_TYPE_NOT_ALLOWED_FOR_API: The data type does not allowed to register listeners."
            FitnessStatusCodes.REQUIRES_APP_WHITELISTING -> "REQUIRES_APP_WHITELISTING: The app is black-listed by Google."
            else -> null
        }
    override val stringRes: Int
        get() = R.string.error_google_api_exception
}

class AbcFirebaseException(message: String?): AbcException(message) {

}

class FirebaseInvalidUserException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_invalid_user
}

class FirebaseInvalidCredentialException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_invalid_user
}

class FirebaseUserCollisionException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_collision
}

class HttpRequestException(message: String?) : AbcException(message) {
    override val stringRes: Int
        get() = R.string.error_http_error
}

class NoSignedGoogleAccountException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_no_signed_google_account
}

class InvalidUrlException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_invalid_url
}

class PolarH10Exception(message: String?) : AbcException(message) {
    override val stringRes: Int
        get() = R.string.error_polar_h10
}

class GooglePlayServiceOutdatedException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_require_to_update_google_play_service
}

class SurveyIncorrectlyAnsweredException : AbcException() {
    override val stringRes: Int
        get() = R.string.error_survey_incorrectly_answered
}