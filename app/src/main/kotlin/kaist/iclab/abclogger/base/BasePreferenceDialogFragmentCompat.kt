package kaist.iclab.abclogger.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class BasePreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {
    protected val TAG : String = javaClass.simpleName

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach()")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated()")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach()")
    }

    override fun onCreateDialogView(context: Context?): View {
        Log.d(TAG, "onCreateDialogView()")
        return super.onCreateDialogView(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog()")
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onBindDialogView(view: View?) {
        Log.d(TAG, "onBindDialogView()")
        super.onBindDialogView(view)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        Log.d(TAG, "onDialogClosed()")
    }
}