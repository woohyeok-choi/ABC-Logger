package kaist.iclab.abclogger.common.type

enum class LoadStatus {
        INIT,
        RUNNING,
        SUCCESS,
        FAILED
    }

    data class LoadState (val status: LoadStatus = LoadStatus.INIT, val error: Throwable? = null) {
        companion object {
            val INIT = LoadState(LoadStatus.INIT)
            val LOADING = LoadState(LoadStatus.RUNNING)
            val LOADED = LoadState(LoadStatus.SUCCESS)
            fun ERROR(throwable: Throwable?) = LoadState(LoadStatus.FAILED, throwable)
        }
    }