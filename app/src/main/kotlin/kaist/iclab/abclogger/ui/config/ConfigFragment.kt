package kaist.iclab.abclogger.ui.config

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseFragment
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kotlinx.android.synthetic.main.fragment_config.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment() {
    private val viewModel : ConfigViewModel by viewModel()
    private lateinit var binding : FragmentConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_config, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.load()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}