package kaist.iclab.abclogger

abstract class ABCException: Exception() {
    abstract val stringRes : Int
}

class GeneralException : ABCException() {
    override val stringRes: Int
        get() = R.string.error_general_error
}

class GoogleSignInException : ABCException() {
    override val stringRes: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class FirebaseAuthFailureException: ABCException() {
    override val stringRes: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}