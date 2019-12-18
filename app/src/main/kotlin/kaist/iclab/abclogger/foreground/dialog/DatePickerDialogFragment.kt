package kaist.iclab.abclogger.foreground.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.widget.DatePicker
import java.util.*

class DatePickerDialogFragment: androidx.fragment.app.DialogFragment(), DatePickerDialog.OnDateSetListener {
    interface OnDateSetListener {
        fun onDateSet(year: Int, month: Int, day: Int)
    }
    private var listener: OnDateSetListener? = null

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        listener?.onDateSet(year, month + 1, dayOfMonth)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (activity is OnDateSetListener) {
            listener = activity as OnDateSetListener
        } else if (targetFragment is OnDateSetListener) {
            listener = targetFragment as OnDateSetListener
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return GregorianCalendar.getInstance().let {
            DatePickerDialog(requireContext(), this, it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
        }
    }
}