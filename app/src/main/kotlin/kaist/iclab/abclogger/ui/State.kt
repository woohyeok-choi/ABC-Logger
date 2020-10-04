package kaist.iclab.abclogger.ui

sealed class State {
    object Loading : State()
    class Success<T>(val data: T) : State()
    class Failure(val error: Throwable?) : State()
}
