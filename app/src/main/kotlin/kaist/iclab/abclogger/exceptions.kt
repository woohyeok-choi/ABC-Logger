package kaist.iclab.abclogger

import com.google.android.gms.common.api.CommonStatusCodes

abstract class ABCException (override val message: String?): Exception(message) {
    constructor() : this(null)
    abstract val stringRes : Int
}

class GeneralException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_general
}

class GoogleSignInException : ABCException() {
    override val stringRes: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class FirebaseAuthFailureException: ABCException() {
    override val stringRes: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
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

