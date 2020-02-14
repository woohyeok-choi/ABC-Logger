package kaist.iclab.abclogger.ui.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import kaist.iclab.abclogger.commons.AppLog
import kaist.iclab.abclogger.R

abstract class BaseCustomViewDialogFragment<T : ViewDataBinding, VM : BaseViewModel<*>> : DialogFragment() {
    protected val TAG: String = javaClass.simpleName

    @get:LayoutRes
    abstract val layoutId: Int
    abstract val viewModelVariable: Int
    abstract val viewModel: VM
    lateinit var dataBinding: T
    @get:StringRes
    abstract val titleRes: Int

    abstract fun beforeExecutePendingBindings()

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppLog.d(TAG, "onCreate()")
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(TAG, "onCreate()")
    }

    @CallSuper
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())

        dataBinding = DataBindingUtil.inflate(inflater, layoutId, null, false)
        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()
        viewModel.load(arguments)

        return AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setCancelable(false)
                .setView(dataBinding.root)
                .setNeutralButton(R.string.general_close) { _, _ -> dismiss() }.create()
    }

    @CallSuper
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AppLog.d(TAG, "onActivityCreated()")
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        AppLog.d(TAG, "onStart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        AppLog.d(TAG, "onResume()")
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        AppLog.d(TAG, "onPause()")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        AppLog.d(TAG, "onStop()")
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        AppLog.d(TAG, "onDestroyView()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(TAG, "onDestroy()")
    }

    @CallSuper
    override fun onDetach() {
        super.onDetach()
        AppLog.d(TAG, "onDetach()")
    }
}