package kaist.iclab.abclogger.ui.splash

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.AuthRepository
import kaist.iclab.abclogger.core.CollectorRepository
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.sync.HeartBeatRepository
import kaist.iclab.abclogger.databinding.ActivitySplashBinding
import kaist.iclab.abclogger.ui.base.BaseActivity
import kaist.iclab.abclogger.ui.main.MainActivity
import org.koin.android.ext.android.inject
import java.lang.Exception


class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val collectorRepository: CollectorRepository by inject()

    override fun getViewBinding(inflater: LayoutInflater): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater)

    override fun afterViewInflate() {
        lifecycleScope.launchWhenCreated {
            if (intent?.getBooleanExtra(EXTRA_SIGN_OUT, false) == true) {
                AuthRepository.signOut(this@SplashActivity)
                collectorRepository.stop(this@SplashActivity)
                HeartBeatRepository.stop(this@SplashActivity)
            }

            try {
                /**
                 * Step 1: Google Play Service version check.
                 */
                AuthRepository.updateGooglePlayService(this@SplashActivity)
                /**
                 * Step 2: Sign in with Google Account
                 */
                // need to change all getActivityResult calls after update AndroidX lifecycle libraries.
                val signInResult = getActivityResult(
                    AuthRepository.getGoogleSignInIntent(this@SplashActivity),
                    ActivityResultContracts.StartActivityForResult()
                ).data

                /**
                 * Step 3: Authorize Firebase with Google Account
                 */
                AuthRepository.authorizeFirebaseWithGoogleAccount(signInResult)?.email?.let {
                    Log.setUserId(it)
                }

                /**
                 * Step 4: Permission Request
                 */
                if (!isPermissionGranted(this@SplashActivity, collectorRepository.permissions)) {
                    val results = getActivityResult(
                        collectorRepository.permissions.toTypedArray(),
                        ActivityResultContracts.RequestMultiplePermissions()
                    )

                    if (results.values.any { !it }) throw PreconditionError.permissionDenied()
                }

                /**
                 * Step 5: Special permission: Background Location Access
                 * For < 29, it is not required
                 * For 29, it shows normal permission dialog
                 * For > 30, it shows special setting activity.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!CollectorRepository.isBackgroundLocationAccessGranted(this@SplashActivity)) {
                        val result = getActivityResult(
                            CollectorRepository.getBackgroundLocationPermission(),
                            ActivityResultContracts.RequestPermission()
                        )
                        if (!result) throw PreconditionError.backgroundLocationAccessDenied()
                    }
                }

                /**
                 * Step 6: Whitelist Request
                 */
                if (!CollectorRepository.isBatteryOptimizationIgnored(this@SplashActivity)) {
                    getActivityResult(
                        CollectorRepository.getIgnoreBatteryOptimizationIntent(this@SplashActivity),
                        ActivityResultContracts.StartActivityForResult()
                    )
                }
                if (!CollectorRepository.isBatteryOptimizationIgnored(this@SplashActivity))
                    throw PreconditionError.whiteListDenied()

                SystemClock.sleep(1000)
                finish()
                startActivity(
                    Intent(
                        this@SplashActivity,
                        MainActivity::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                SystemClock.sleep(1000)
                Log.e(this@SplashActivity.javaClass, AbcError.wrap(e))
                showToast(AbcError.wrap(e).toSimpleString(this@SplashActivity))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_SIGN_OUT = "${BuildConfig.APPLICATION_ID}.ui.splash.SplashActivity.SIGN_OUT"
    }
}