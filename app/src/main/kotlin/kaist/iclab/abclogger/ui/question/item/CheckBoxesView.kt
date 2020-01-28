package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import kaist.iclab.abclogger.R

class CheckBoxesView (context: Context, attrs: AttributeSet?) : QuestionView(context, attrs) {
    private val layoutCheckGroup = LinearLayout(context).apply {
        id = View.generateViewId()
        orientation = LinearLayout.VERTICAL
    }

    private val btnEtc : CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { _, isChecked ->
            edtEtc.isEnabled = isChecked
            attrChanged?.onChange()
        }
    }

    private val edtEtc: EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> attrChanged?.onChange() }, {})
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
        (layoutCheckGroup.children + btnEtc + edtEtc).forEach { view -> view.isEnabled = isAvailable }
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
            }
        }.forEach { button ->
            layoutCheckGroup.addView(button)
        }
    }

    override var responses: Array<String> = arrayOf()
        get() =
            (layoutCheckGroup.children + btnEtc).filter { view ->
                (view as? CompoundButton)?.isChecked == true
            }.mapNotNull { view ->
                val text = if (view == btnEtc) {
                    btnEtc.text?.toString()
                } else {
                    (view as? CompoundButton)?.text
                }
                if (!text.isNullOrBlank()) text.toString() else null
            }.toSet().toTypedArray()
        set(value) {
            if (field.toSet() == value.toSet()) return

            value.firstOrNull { text ->
                val checkBox = layoutCheckGroup.children.find { view ->
                    (view as? CompoundButton)?.text == text
                }
                (checkBox as? CompoundButton)?.isChecked = true

                checkBox == null
            }?.let { text ->
                btnEtc.isChecked = true
                edtEtc.setText(text)
            }
        }
}