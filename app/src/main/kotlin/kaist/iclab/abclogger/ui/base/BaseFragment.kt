package kaist.iclab.abclogger.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import kaist.iclab.abclogger.commons.AppLog

abstract class BaseFragment<T : ViewDataBinding, VM : ViewModel> : Fragment() {
    protected val TAG: String = javaClass.simpleName

    @get:LayoutRes
    abstract val layoutId: Int
    abstract val viewModelVariable: Int
    abstract val viewModel: VM
    lateinit var dataBinding: T

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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        AppLog.d(TAG, "onCreateView()")
        dataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        return dataBinding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLog.d(TAG, "onViewCreated()")
        dataBinding.setVariable(viewModelVariable, viewModel)

        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()

        (viewModel as? BaseViewModel<*>)?.load(arguments)

        dataBinding.executePendingBindings()
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