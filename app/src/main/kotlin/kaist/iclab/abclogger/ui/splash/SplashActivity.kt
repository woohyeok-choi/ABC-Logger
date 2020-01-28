package kaist.iclab.abclogger.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.*
import com.tedpark.tedpermission.rx2.TedRx2Permission
import io.reactivex.subjects.SingleSubject
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.ui.main.MainActivity
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import java.lang.Exception


class SplashActivity : BaseAppCompatActivity() {
    init {
        lifecycleScope.launchWhenCreated {
            val isSuccessful = try {

                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this@SplashActivity).toCoroutine()
                requestGoogleSignIn()

                val intent = googleSignInSingle.toCoroutine()
                val account = GoogleSignIn.getSignedInAccountFromIntent(intent).toCoroutine() ?: throw NoSignedGoogleAccountException()
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val user = FirebaseAuth.getInstance().signInWithCredential(credential).toCoroutine()?.user ?: throw FirebaseInvalidCredentialException()
                val message = listOf(
                        getString(R.string.msg_sign_in_success), user.displayName
                        ?: user.email ?: ""
                ).joinToString(separator = " ")

                val permissionResult = TedRx2Permission.with(this@SplashActivity)
                        .setRationaleTitle(getString(R.string.dialog_title_permission_request))
                        .setRationaleMessage(getString(R.string.dialog_message_permission_request))
                        .setPermissions(*abc.getAllRequiredPermissions().toTypedArray())
                        .request().toCoroutine()

                val isPermitted = if(permissionResult?.isGranted != true) {
                    showToast(R.string.msg_permission_setting_required, false)

                    requestPermissionSetting()
                    permissionSettingSingle.toCoroutine()
                } else {
                    true
                }

                if (!isPermitted) throw PermissionDeniedException()

                val isWhitelisted = if(!this@SplashActivity.isWhitelisted()) {

                    requestWhiteList()
                    whiteListSingle.toCoroutine()
                } else {
                    true
                }

                if (!isWhitelisted) throw WhiteListDeniedException()
                showToast(message, true)
                true
            } catch (e: Exception) {
                showToast(e, false)
                false
            }

            delay(1000)
            finish()

            if(isSuccessful) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                this@SplashActivity.startActivity(intent)
            }
        }
    }

    private val abc: ABC by inject()

    private val permissionSettingSingle = SingleSubject.create<Boolean>()
    private val whiteListSingle = SingleSubject.create<Boolean>()
    private val googleSignInSingle = SingleSubject.create<Intent>()

    private fun requestPermissionSetting() = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.parse("package:${packageName}")
    }.let { intent -> startActivityForResult(intent, REQUEST_CODE_PERMISSION_SETTING) }

    @SuppressLint("BatteryLife")
    private fun requestWhiteList() = Intent().apply {
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:${packageName}")
    }.let { intent -> startActivityForResult(intent, REQUEST_CODE_WHITE_LIST) }

    private fun requestGoogleSignIn() = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build().let { options ->
                startActivityForResult(GoogleSignIn.getClient(this, options).signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_PERMISSION_SETTING -> permissionSettingSingle.onSuccess(checkPermission(abc.getAllRequiredPermissions()))
            REQUEST_CODE_WHITE_LIST -> whiteListSingle.onSuccess(isWhitelisted())
            REQUEST_CODE_GOOGLE_SIGN_IN -> googleSignInSingle.onSuccess(data ?: Intent())
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION_SETTING = 0x09
        const val REQUEST_CODE_WHITE_LIST = 0x10
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x11
    }
}