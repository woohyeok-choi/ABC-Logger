package kaist.iclab.abclogger.ui

import kaist.iclab.abclogger.commons.ABCException

data class Status(val state: Int, val error: ABCException? = null) {
    companion object {
        const val STATE_INIT = 0
        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1

        fun init() = Status(STATE_INIT)
        fun loading() = Status(STATE_LOADING)
        fun success() = Status(STATE_SUCCESS)
        fun failure(t: Throwable? = null) = Status(STATE_FAILURE, when (t) {
            is ABCException -> t
            else -> ABCException.wrap(t)
        })
    }

    fun isSucceeded(): Boolean = state == STATE_SUCCESS
    fun isLoading(): Boolean = state == STATE_LOADING
    fun isFailed(): Boolean = state == STATE_FAILURE
}
