package kaist.iclab.abclogger.core.ui

import androidx.viewbinding.ViewBinding

abstract class BaseViewModelActivity<T : ViewBinding, VM : BaseViewModel> : BaseActivity<T>() {
    abstract val viewModel: VM
}