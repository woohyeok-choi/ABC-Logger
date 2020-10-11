package kaist.iclab.abclogger.commons

import android.content.*
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.coroutines.*

fun View.setHorizontalPadding(pixelSize: Int) {
    val topPadding = paddingTop
    val bottomPadding = paddingBottom
    setPadding(pixelSize, topPadding, pixelSize, bottomPadding)
}

fun View.setVerticalPadding(pixelSize: Int) {
    val leftPadding = paddingLeft
    val rightPadding = paddingRight
    setPadding(leftPadding, pixelSize, rightPadding, pixelSize)
}

fun Context.showSnackBar(
    view: View,
    messageRes: Int,
    showAlways: Boolean = false,
    actionRes: Int? = null,
    action: (() -> Unit)? = null
) {

    var snackBar = Snackbar.make(
        view,
        messageRes,
        if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
    )
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }
    snackBar.show()
}

fun Context.showSnackBar(
    view: View,
    message: String,
    showAlways: Boolean = false,
    actionRes: Int? = null,
    action: (() -> Unit)? = null
) {
    var snackBar = Snackbar.make(
        view,
        message,
        if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
    )
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }

    snackBar.show()
}


fun Fragment.showSnackBar(
    view: View,
    messageRes: Int,
    showAlways: Boolean = false,
    actionRes: Int? = null,
    action: (() -> Unit)? = null
) {
    var snackBar = Snackbar.make(
        view,
        messageRes,
        if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
    )
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }
    snackBar.show()
}

fun Fragment.showSnackBar(
    view: View,
    message: String,
    showAlways: Boolean = false,
    actionRes: Int? = null,
    action: (() -> Unit)? = null
) {
    var snackBar = Snackbar.make(
        view,
        message,
        if (showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
    )
    if (actionRes != null) {
        snackBar = snackBar.setAction(actionRes) { action?.invoke() }
    }
    snackBar.show()
}


fun Context.showToast(messageRes: Int, isShort: Boolean = true) =
    Toast.makeText(this, messageRes, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Context.showToast(message: String, isShort: Boolean = true) =
    Toast.makeText(this, message, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()


fun Fragment.showToast(messageRes: Int, isShort: Boolean = true) =
    Toast.makeText(
        requireContext(),
        messageRes,
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()


fun Fragment.showToast(message: String, isShort: Boolean = true) =
    Toast.makeText(
        requireContext(),
        message,
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()


fun Context.showToast(throwable: Throwable?, isShort: Boolean = true) {
    val msg = when (throwable) {
        is AbcError -> throwable.toString(this)
        else -> AbcError.wrap(throwable).toString(this)
    }
    Toast.makeText(this, msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Fragment.showToast(throwable: Throwable?, isShort: Boolean = true) {
    val msg = when (throwable) {
        is AbcError -> throwable.toSimpleString(requireContext())
        else -> AbcError.wrap(throwable).toSimpleString(requireContext())
    }
    Toast.makeText(requireContext(), msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        .show()
}

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

fun Intent.fillExtras(vararg params: Pair<String, Any?>): Intent {
    params.forEach { (key, value) ->
        when (value) {
            null -> putExtra(key, null as Serializable?)
            is Int -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is CharSequence -> putExtra(key, value)
            is String -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Char -> putExtra(key, value)
            is Short -> putExtra(key, value)
            is Boolean -> putExtra(key, value)
            is Serializable -> putExtra(key, value)
            is Bundle -> putExtra(key, value)
            is Parcelable -> putExtra(key, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> putExtra(key, value)
                value.isArrayOf<String>() -> putExtra(key, value)
                value.isArrayOf<Parcelable>() -> putExtra(key, value)
            }
            is IntArray -> putExtra(key, value)
            is LongArray -> putExtra(key, value)
            is FloatArray -> putExtra(key, value)
            is DoubleArray -> putExtra(key, value)
            is CharArray -> putExtra(key, value)
            is ShortArray -> putExtra(key, value)
            is BooleanArray -> putExtra(key, value)
        }
    }
    return this
}

fun Context.safeRegisterReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = try {
    registerReceiver(receiver, filter)
} catch (e: IllegalArgumentException) {
}

fun Context.safeUnregisterReceiver(receiver: BroadcastReceiver) = try {
    unregisterReceiver(receiver)
} catch (e: IllegalArgumentException) {
}

fun ContentResolver.safeRegisterContentObserver(
    uri: Uri,
    notifyForDescendants: Boolean,
    observer: ContentObserver
) = try {
    registerContentObserver(uri, notifyForDescendants, observer)
} catch (e: Exception) {
}

fun ContentResolver.safeUnregisterContentObserver(observer: ContentObserver) = try {
    unregisterContentObserver(observer)
} catch (e: Exception) {
}

suspend fun <T> Single<T>.toCoroutine(
    context: CoroutineContext = EmptyCoroutineContext,
    throwable: Throwable? = null
) = withContext(context) {
    suspendCoroutine<T?> { continuation ->
        subscribe { result, exception ->
            if (exception != null) {
                continuation.resumeWithException(throwable ?: exception)
            } else {
                continuation.resume(result)
            }
        }
    }
}

suspend fun <T> Task<T>.toCoroutine(
    context: CoroutineContext = EmptyCoroutineContext,
    throwable: Throwable? = null
) = withContext(context) {
    suspendCoroutine<T?> { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(throwable ?: task.exception ?: Exception())
            }
        }
    }
}

suspend fun <I, O> ActivityResultCaller.getActivityResult(
    input: I,
    contract: ActivityResultContract<I, O>,
    context: CoroutineContext = EmptyCoroutineContext
): O = withContext(context) {
    suspendCoroutine { continuation ->
        registerForActivityResult(contract) {
            continuation.resume(it)
        }.launch(input)
    }
}

suspend fun FragmentActivity.getFragmentResult(requestKey: String, context: CoroutineContext = EmptyCoroutineContext): Bundle = withContext(context) {
    val owner = this@getFragmentResult
    suspendCoroutine { continuation ->
        supportFragmentManager.setFragmentResultListener(
            requestKey,
            owner
        ) { _, bundle ->
            supportFragmentManager.clearFragmentResultListener(requestKey)
            continuation.resume(bundle)
        }
    }
}

suspend fun Fragment.getFragmentResult(requestKey: String, context: CoroutineContext = EmptyCoroutineContext): Bundle = withContext(context) {
    val owner = this@getFragmentResult
    suspendCoroutine { continuation ->
        parentFragmentManager.setFragmentResultListener(
            requestKey,
            owner
        ) { _, bundle ->
            parentFragmentManager.clearFragmentResultListener(requestKey)
            continuation.resume(bundle)
        }
    }
}

fun <A, B, R> Iterable<A>.combination(other: Iterable<B>, transform: (A, B) -> R) : List<R> = flatMap { a ->
        other.map { b ->
            transform.invoke(a, b)
        }
    }


infix fun <T> Collection<T>.strictlyEquals(other: Collection<T>) =
    this.size == other.size && this.containsAll(other) && other.containsAll(this)
