package kaist.iclab.abclogger

abstract class ABCException: Exception() {
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