package kaist.iclab.abclogger.ui.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.SupportErrorDialogFragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.firebase.auth.*
import com.tedpark.tedpermission.rx2.TedRx2Permission
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.SingleSubject
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.ui.main.MainActivity
import org.koin.android.ext.android.inject


class SplashActivity : BaseAppCompatActivity() {
    private val abc: ABCLogger by inject()

    private val permissionSettingSingle = SingleSubject.create<Boolean>()
    private val whiteListSingle = SingleSubject.create<Boolean>()
    private val googleSignInSingle = SingleSubject.create<Intent>()

    private lateinit var disposal: Disposable

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

        disposal = Single.create<Unit> { emitter ->
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
                    .addOnSuccessListener { emitter.onSuccess(Unit) }
                    .addOnFailureListener { emitter.onError(GooglePlayServiceOutdatedException()) }
        }.flatMap {
            TedRx2Permission.with(this)
                    .setRationaleTitle(getString(R.string.dialog_title_permission_request))
                    .setRationaleMessage(getString(R.string.dialog_message_permission_request))
                    .setPermissions(*abc.getAllRequiredPermissions().toTypedArray())
                    .request()
        }.flatMap { result ->
            return@flatMap if (result.isGranted) {
                Single.just<Boolean>(true)
            } else {
                showToast(R.string.msg_permission_setting_required, false)
                requestPermissionSetting()

                permissionSettingSingle
            }
        }.flatMap { isPermitted ->
            if (!isPermitted) throw PermissionDeniedException()

            return@flatMap if (checkWhitelist()) {
                Single.just<Boolean>(true)
            } else {
                requestWhiteList()

                whiteListSingle
            }
        }.flatMap { isWhiteListed ->
            if (!isWhiteListed) throw WhiteListDeniedException()
            requestGoogleSignIn()

            return@flatMap googleSignInSingle
        }.flatMap { intent ->
            Single.create<GoogleSignInAccount> { emitter ->
                GoogleSignIn.getSignedInAccountFromIntent(intent)
                        .addOnSuccessListener { account -> emitter.onSuccess(account) }
                        .addOnFailureListener { emitter.onError(it) }
            }
        }.flatMap { account ->
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            Single.create<FirebaseUser> { emitter ->
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { result ->
                            val user = result.user ?: throw(FirebaseInvalidCredentialException())
                            emitter.onSuccess(user)
                        }.addOnFailureListener { exception -> emitter.onError(exception) }
            }
        }.subscribe { user, exception ->
            if (user != null) {
                val message = listOf(
                        getString(R.string.msg_sign_in_success), user.displayName
                        ?: user.email ?: ""
                ).joinToString(separator = " ")
                showToast(message, true)
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                exception?.printStackTrace()
                showToast(exception, true)
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposal.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val a = if (data?.hasExtra("googleSignInAccount") == true) data.getParcelableExtra<GoogleSignInAccount>("googleSignInAccount") else null
        val s = if (data?.hasExtra("googleSignInStatus") == true) data.getParcelableExtra<Status>("googleSignInStatus") else null

        Log.d(TAG, "data = ${a?.email ?: "NO EMAIL"}/${s?.statusMessage ?: "NO CODE"}")
        when (requestCode) {
            REQUEST_CODE_PERMISSION_SETTING -> permissionSettingSingle.onSuccess(checkPermission(abc.getAllRequiredPermissions()))
            REQUEST_CODE_WHITE_LIST -> whiteListSingle.onSuccess(checkWhitelist())
            REQUEST_CODE_GOOGLE_SIGN_IN -> googleSignInSingle.onSuccess(data ?: Intent())
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION_SETTING = 0x09
        const val REQUEST_CODE_WHITE_LIST = 0x10
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x11
    }
}