package kaist.iclab.abclogger.base

import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding

abstract class BaseViewModelActivity<T : ViewBinding, VM : BaseViewModel> : BaseActivity<T>() {
    abstract val viewModel: VM
}