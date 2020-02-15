package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.observe
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.ui.base.BaseFragment
import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.splash.SplashActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ConfigFragment : BaseFragment<FragmentConfigBinding, ConfigViewModel>(),
        SharedPreferences.OnSharedPreferenceChangeListener, ConfigNavigator {
    override val layoutId: Int = R.layout.fragment_config

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: ConfigViewModel by viewModel { parametersOf(this) }

    override fun beforeExecutePendingBindings() {
        val adapter = ConfigListAdapter()

        dataBinding.recyclerView.adapter = adapter
        dataBinding.recyclerView.itemAnimator = null

        viewModel.configs.observe(this) { data ->
            data?.let { adapter.items = data }
        }
    }

    override fun onStart() {
        super.onStart()
        context?.getSharedPreferences(
                BuildConfig.PREF_NAME,
                Context.MODE_PRIVATE
        )?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        context?.getSharedPreferences(
                BuildConfig.PREF_NAME,
                Context.MODE_PRIVATE
        )?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        viewModel.load()
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
    }

    override fun navigateIntent(intent: Intent) {
        startActivityForResult(intent, 0xFF)
    }

    override fun navigateBeforeFlush() {
        YesNoDialogFragment.showDialog(
                parentFragmentManager,
                getString(R.string.dialog_title_flush_data),
                getString(R.string.dialog_message_flush_data)
        ) { viewModel.flush() }
    }

    override fun navigateBeforeLogout() {
        YesNoDialogFragment.showDialog(
                parentFragmentManager,
                getString(R.string.dialog_title_sign_out),
                getString(R.string.dialog_message_sign_out)
        ) { viewModel.logout() }
    }

    override fun navigateAfterLogout() {
        val intent = Intent(requireContext(), SplashActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.load()
    }
}