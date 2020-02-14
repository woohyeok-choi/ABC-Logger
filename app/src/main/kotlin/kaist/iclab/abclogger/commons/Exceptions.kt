package kaist.iclab.abclogger.commons

import android.content.Context
import com.github.kittinunf.fuel.core.FuelError
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kaist.iclab.abclogger.R
import polar.com.sdk.api.errors.*

abstract class ABCException (override val message: String?): Exception(message) {
    constructor() : this(null)

    abstract val stringRes : Int

    fun toString(context: Context): String = listOf(
            context.getString(stringRes), message
    ).filter { !it.isNullOrEmpty() }.joinToString(separator = ": ")

    companion object {
        fun wrap(t: Throwable?) = when(t) {
            is ApiException -> GoogleApiException(t.statusCode)
            is FirebaseAuthInvalidUserException -> FirebaseInvalidUserException()
            is FirebaseAuthInvalidCredentialsException -> FirebaseInvalidCredentialException()
            is FirebaseAuthUserCollisionException -> FirebaseUserCollisionException()
            is FuelError -> HttpRequestException(t.message)
            is PolarDeviceDisconnected -> PolarH10Exception("Device is disconnected.")
            is PolarDeviceNotFound -> PolarH10Exception("Device is not found.")
            is PolarDeviceNotConnected -> PolarH10Exception("Device is not connected.")
            is PolarInvalidArgument -> PolarH10Exception("Device id is invalid.")
            is PolarServiceNotAvailable -> PolarH10Exception("Polar service is not available..")
            is ABCException -> t
            else -> UnhandledException(t?.message)
        }
    }
}

class UnhandledException(message: String?) : ABCException(message) {
    override val stringRes: Int
        get() = R.string.error_general
}

class EmptySurveyException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_empty_survey
}

class InvalidSurveyFormatException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_invalid_survey_format
}

class InvalidEntityIdException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_invalid_entity_id
}

class PermissionDeniedException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_permission_denied
}

class WhiteListDeniedException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_not_white_listed
}

class GoogleApiException(private val statusCode: Int) : ABCException() {
    override val message: String?
        get() = when(statusCode) {
            CommonStatusCodes.API_NOT_CONNECTED -> "API is not connected."
            CommonStatusCodes.CANCELED -> "Request is canceled."
            CommonStatusCodes.DEVELOPER_ERROR -> "API is incorrectly configured."
            CommonStatusCodes.ERROR -> "Unknown error occurs."
            CommonStatusCodes.INTERNAL_ERROR -> "Internal error occurs"
            CommonStatusCodes.INTERRUPTED-> "Request is interrupted."
            CommonStatusCodes.INVALID_ACCOUNT -> "Invalid account name."
            CommonStatusCodes.NETWORK_ERROR -> "Network error occurs."
            CommonStatusCodes.RESOLUTION_REQUIRED -> "Resolution is required."
            CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign-in is required."
            CommonStatusCodes.TIMEOUT -> "Request is timed out."
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in canceled."
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress."
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed. There may be no Google account connected to this device, or Google Play service is outdated."
            else -> null
        }
    override val stringRes: Int
        get() = R.string.error_google_api_exception
}

class FirebaseInvalidUserException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_invalid_user
}

class FirebaseInvalidCredentialException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_invalid_user
}

class FirebaseUserCollisionException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_firebase_auth_collision
}

class HttpRequestException(message: String?) : ABCException(message) {
    override val stringRes: Int
        get() = R.string.error_http_error
}

class NoSignedGoogleAccountException: ABCException() {
    override val stringRes: Int
        get() = R.string.error_no_signed_google_account
}

class InvalidUrlException: ABCException() {
    override val stringRes: Int
        get() = R.string.error_invalid_url
}

class PolarH10Exception(message: String?): ABCException(message) {
    override val stringRes: Int
        get() = R.string.error_polar_h10
}

class GooglePlayServiceOutdatedException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_require_to_update_google_play_service
}

class SurveyIncorrectlyAnsweredException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_survey_incorrectly_answered
}