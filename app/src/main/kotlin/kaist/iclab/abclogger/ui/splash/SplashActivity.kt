package kaist.iclab.abclogger.ui.splash

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.*
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.ActivitySplashBinding
import kaist.iclab.abclogger.ui.base.BaseActivity
import kaist.iclab.abclogger.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>(), SplashNavigator {
    override val layoutId: Int = R.layout.activity_splash

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: SplashViewModel by viewModel { parametersOf(this, this) }

    override fun beforeExecutePendingBindings() {
        viewModel.doGoogleSignIn()
    }

    override fun navigateGoogleSignIn(intent: Intent) {
        startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    override fun navigateFirebaseAuth(user: FirebaseUser) {
        lifecycleScope.launch(Dispatchers.Main) {
            val msg = listOf(
                    getString(R.string.msg_sign_in_success),
                    user.displayName ?: user.email ?: ""
            ).joinToString(" ")

            showToast(msg)

            viewModel.doPermissions(
                    title = getString(R.string.dialog_title_permission_request),
                    message = getString(R.string.dialog_message_permission_request)
            )
        }

    }

    override fun navigatePermission(isGranted: Boolean, intent: Intent) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (isGranted) {
                viewModel.doWhiteList()
            } else {
                showToast(R.string.msg_permission_setting_required, false)
                startActivityForResult(intent, REQUEST_CODE_PERMISSION_SETTING)
            }
        }

    }

    override fun navigatePermissionAgain() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.doWhiteList()
        }

    }

    override fun navigateWhiteList(intent: Intent) {
        lifecycleScope.launch(Dispatchers.Main) {
            startActivityForResult(intent, REQUEST_CODE_WHITE_LIST)
        }
    }

    override fun navigateSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        lifecycleScope.launch {
            delay(1000)
            finish()
            startActivity(intent)
        }
    }

    override fun navigateError(throwable: Throwable) {
        lifecycleScope.launch {
            showToast(throwable)
            delay(1000)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_SIGN_IN -> viewModel.doFirebaseAuth(data)
            REQUEST_CODE_PERMISSION_SETTING -> viewModel.doPermissionsAgain()
            REQUEST_CODE_WHITE_LIST -> viewModel.doWhiteListAgain()
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION_SETTING = 0x09
        const val REQUEST_CODE_WHITE_LIST = 0x10
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x11
    }
}