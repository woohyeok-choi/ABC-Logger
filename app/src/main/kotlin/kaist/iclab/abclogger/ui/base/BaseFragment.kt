package kaist.iclab.abclogger.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import kaist.iclab.abclogger.core.Log

abstract class BaseFragment<T : ViewBinding> : Fragment() {
    lateinit var viewBinding: T

    protected abstract fun initView(viewBinding: T)
    protected abstract fun afterViewCreated(viewBinding: T)
    protected abstract fun getViewBinding(inflater: LayoutInflater): T

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(javaClass, "onAttach()")
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(javaClass, "onCreate()")
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(javaClass, "onCreateView()")
        viewBinding = getViewBinding(inflater)
        initView(viewBinding)
        return viewBinding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(javaClass, "onViewCreated()")
        afterViewCreated(viewBinding)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        Log.d(javaClass, "onStart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        Log.d(javaClass, "onResume()")

    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        Log.d(javaClass, "onPause()")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        Log.d(javaClass, "onStop()")
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(javaClass, "onDestroyView()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass, "onDestroy()")
    }

    @CallSuper
    override fun onDetach() {
        super.onDetach()
        Log.d(javaClass, "onDetach()")
    }
}