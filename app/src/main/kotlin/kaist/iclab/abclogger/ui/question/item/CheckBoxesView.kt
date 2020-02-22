package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import kaist.iclab.abclogger.R

class CheckBoxesView(context: Context, attrs: AttributeSet?) : QuestionView(context, attrs) {
    private val layoutCheckGroup = LinearLayout(context).apply {
        id = View.generateViewId()
        orientation = LinearLayout.VERTICAL
    }

    private val btnEtc: CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { view, isChecked ->
            edtEtc.isEnabled = isChecked && view.isEnabled
            edtEtc.hint = if (edtEtc.isEnabled) context.getString(R.string.general_free_text) else null
            attrChanged?.onChange()
        }
    }

    private val edtEtc: TextInputEditText = TextInputEditText(context).apply {
        id = View.generateViewId()
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> attrChanged?.onChange() }, {})
        isEnabled = false
        hint = null
    }

    init {
        addView(layoutCheckGroup, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(btnEtc, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(edtEtc, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().also { constraint ->
            constraint.clone(this)

            constraint.connect(layoutCheckGroup.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraint.connect(layoutCheckGroup.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraint.connect(layoutCheckGroup.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            constraint.connect(btnEtc.id, ConstraintSet.TOP, layoutCheckGroup.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.BOTTOM, edtEtc.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

            constraint.connect(edtEtc.id, ConstraintSet.TOP, layoutCheckGroup.id, ConstraintSet.BOTTOM)
            constraint.connect(edtEtc.id, ConstraintSet.LEFT, btnEtc.id, ConstraintSet.RIGHT)
            constraint.connect(edtEtc.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }.applyTo(this)
    }

    override fun setAvailable(isAvailable: Boolean) {
        (layoutCheckGroup.children + btnEtc).forEach { view -> view.isEnabled = isAvailable }
    }

    override fun setShowEtc(showEtc: Boolean) {
        btnEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
        edtEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
    }

    override fun setOptions(options: Array<String>) {
        layoutCheckGroup.removeAllViews()

        options.map { option ->
            CheckBox(context).apply {
                id = View.generateViewId()
                text = option
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
                setOnCheckedChangeListener { _, _ -> attrChanged?.onChange() }
            }
        }.forEach { button ->
            layoutCheckGroup.addView(button)
        }
    }

    override var responses: Array<String> = arrayOf()
        get() {
            val checkedItems = layoutCheckGroup.children.map { view ->
                if ((view as? CompoundButton)?.isChecked == true) {
                    view.text?.toString()
                } else {
                    null
                }
            } + if (btnEtc.isChecked) edtEtc.text?.toString() else null
            return checkedItems.filterNotNull().filter { !it.isBlank() }.toSet().toTypedArray()
        }
        set(value) {
            if (field.contentEquals(value)) return

            value.firstOrNull { text ->
                val checkBox = layoutCheckGroup.children.find { view ->
                    (view as? CompoundButton)?.text == text
                } as? CompoundButton
                checkBox?.isChecked = true
                checkBox == null
            }?.let { text ->
                btnEtc.isChecked = true
                edtEtc.setText(text)
            }
        }
}