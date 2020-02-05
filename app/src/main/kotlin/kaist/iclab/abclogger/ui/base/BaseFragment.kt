package kaist.iclab.abclogger.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kaist.iclab.abclogger.AppLog

open class BaseFragment: Fragment() {
    protected val TAG: String = javaClass.simpleName

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppLog.d(TAG, "onCreate()")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(TAG, "onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        AppLog.d(TAG, "onCreateView()")

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLog.d(TAG, "onViewCreated()")

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AppLog.d(TAG, "onActivityCreated()")
    }

    override fun onStart() {
        super.onStart()
        AppLog.d(TAG, "onStart()")
    }

    override fun onResume() {
        super.onResume()
        AppLog.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        AppLog.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        AppLog.d(TAG, "onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLog.d(TAG, "onDestroyView()")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(TAG, "onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        AppLog.d(TAG, "onDetach()")
    }
}