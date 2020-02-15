package kaist.iclab.abclogger.ui.splash

import android.content.Intent
import android.os.SystemClock
import com.google.firebase.auth.FirebaseUser
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.ActivitySplashBinding
import kaist.iclab.abclogger.ui.base.BaseActivity
import kaist.iclab.abclogger.ui.main.MainActivity
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

    override fun navigatePermission(isGranted: Boolean, intent: Intent) {
        if (isGranted) {
            viewModel.doWhiteList()
        } else {
            showToast(R.string.msg_permission_setting_required, false)
            startActivityForResult(intent, REQUEST_CODE_PERMISSION_SETTING)
        }
    }

    override fun navigatePermissionAgain() {
        viewModel.doWhiteList()
    }

    override fun navigateWhiteList(intent: Intent) {
        startActivityForResult(intent, REQUEST_CODE_WHITE_LIST)
    }

    override fun navigateSuccess() {
        SystemClock.sleep(1000)
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
        SystemClock.sleep(1000)
        finish()
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