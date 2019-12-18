package kaist.iclab.abclogger.common.util

import android.graphics.BitmapFactory
import android.os.Handler
import androidx.core.content.ContextCompat
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import kaist.iclab.abclogger.R

fun CircularProgressButton.startWith(block: (() -> Unit)? = null) {
    if (!this.isAnimating) this.startAnimation()
    block?.invoke()
}

fun CircularProgressButton.succeedWith(isReverted: Boolean, block: (() -> Unit)? = null) {
    this.doneLoadingAnimation(
        ContextCompat.getColor(this.context, R.color.colorPrimary),
        BitmapFactory.decodeResource(this.context.resources, R.drawable.baseline_done_white_48)
    )

    if(isReverted) {
        Handler().postDelayed( {
            this.revertAnimation()
        }, 500)
        Handler().postDelayed({
            block?.invoke()
        }, 1000)
    } else {
        Handler().postDelayed({
            block?.invoke()
        }, 1000)
    }
}

fun CircularProgressButton.failedWith(block : (() -> Unit)? = null) {
    this.doneLoadingAnimation(
        ContextCompat.getColor(this.context, R.color.colorAccent),
        BitmapFactory.decodeResource(this.context.resources, R.drawable.baseline_error_outline_white_48)
    )
    block?.invoke()
    Handler().postDelayed({
        this.revertAnimation()
    }, 2000)
}
