package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

abstract class QuestionView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    constructor(context: Context) : this(context, null)
    private var isBound: Boolean = false
    var attrChanged: InverseBindingListener? = null

    abstract fun setAvailable(isAvailable: Boolean)
    abstract fun setShowEtc(showEtc: Boolean)
    abstract fun setOptions(options: Array<String>)
    abstract var responses: Array<String>

    fun bind(options: Array<String>, isAvailable: Boolean, showEtc: Boolean, initResponses: Array<String>) {
        if (isBound) return

        setOptions(options)
        setAvailable(isAvailable)
        setShowEtc(showEtc)
        responses = initResponses

        isBound = true
    }


}

@BindingAdapter("options", "isAvailable", "showEtc", "responses")
fun bindQuestionView(view: QuestionView,
                     options: Array<String>?,
                     isAvailable: Boolean?,
                     showEtc: Boolean?,
                     responses: Array<String>?) {
    view.bind(
            options ?: arrayOf(),
            isAvailable ?: true,
            showEtc ?: true,
            responses ?: arrayOf()
    )
}


@InverseBindingAdapter(attribute = "responses")
fun getResponse(view: QuestionView) = view.responses


@BindingAdapter("responsesAttrChanged")
fun setListener(view: QuestionView, responsesAttrChanged: InverseBindingListener) {
    view.attrChanged = responsesAttrChanged
}