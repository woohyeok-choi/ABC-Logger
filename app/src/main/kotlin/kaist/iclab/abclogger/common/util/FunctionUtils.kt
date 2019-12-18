package kaist.iclab.abclogger.common.util

object FunctionUtils {
    fun <T: Any> checkAllNotNull (vararg elements: T?) : Boolean {
        return elements.all {
            it != null
        }
    }

    fun <T1, T2> runIfAllNotNull(t1: T1?, t2: T2?, block: (T1, T2) -> Unit) {
        if (t1 != null && t2 != null) block(t1, t2)
    }

    fun <T> takeIfNotNull(t: T?, predicate: (T) -> Boolean, blockIfTrue: (T) -> Unit, blockIfFalse: () -> Unit) {
        if(t != null && predicate(t)) {
            blockIfTrue(t)
        } else {
            blockIfFalse()
        }
    }

    fun <T1, T2> takeIfAllNotNull(t1: T1?, t2: T2?, predicate: (T1, T2) -> Boolean, blockIfTrue: (T1, T2) -> Unit, blockIfFalse: () -> Unit) {
        if(t1 != null && t2 != null && predicate(t1, t2)) {
            blockIfTrue(t1, t2)
        } else {
            blockIfFalse()
        }
    }

    fun <T : Any> takeIfAllNotNull(vararg elements: T?,
                                   predicate: (Array<out T>) -> Boolean,
                                   blockIfTrue: (Array<out T>) -> Unit,
                                   blockIfFalse: () -> Unit) {
        try {
            if(predicate(elements.requireNoNulls())) {
                blockIfTrue(elements.requireNoNulls())
            } else {
                blockIfFalse()
            }
        } catch (e: IllegalArgumentException) {
            blockIfFalse()
        }
    }

    fun <T: Comparable<T>> fixInRange(from: T, to: T, value: T): T {
        if(value < from) {
            return from
        } else if (value > to) {
            return to
        }
        return value
    }
}