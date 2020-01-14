package kaist.iclab.abclogger.ui

data class Status(val state: Int, val error: Exception? = null) {
    companion object {
        const val STATE_INIT = 0
        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1

        fun init() = Status(STATE_INIT)
        fun loading() = Status(STATE_LOADING)
        fun success() = Status(STATE_SUCCESS)
        fun failure(e: Exception? = null) = Status(STATE_FAILURE, e)
    }
}
