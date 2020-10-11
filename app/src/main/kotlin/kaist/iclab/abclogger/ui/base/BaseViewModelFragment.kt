package kaist.iclab.abclogger.ui.base

import androidx.viewbinding.ViewBinding

abstract class BaseViewModelFragment<T : ViewBinding, VM : BaseViewModel> : BaseFragment<T>() {
    abstract val viewModel: VM
}