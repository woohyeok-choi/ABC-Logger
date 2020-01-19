package kaist.iclab.abclogger.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    private val abc: ABC by inject()

    private val permissionSettingSingle = SingleSubject.create<Boolean>()
    private val whiteListSingle = SingleSubject.create<Boolean>()
    private val googleSignInSingle = SingleSubject.create<Intent>()
    private val firebaseAuthSingle = SingleSubject.create<FirebaseUser>()

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
                GoogleSignIn.getClient(this, options).signInIntent
            }.let { intent ->
                startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN)
            }

    private fun requestFirebaseAuthorize(data: Intent) = GoogleSignIn.getSignedInAccountFromIntent(data)
            .continueWithTask { task ->
                val credential = GoogleAuthProvider.getCredential(task.result?.idToken, null)

                FirebaseAuth.getInstance().signInWithCredential(credential)
            }.addOnSuccessListener { result ->
                val user = result.user ?: throw FirebaseInvalidCredentialException()
                firebaseAuthSingle.onSuccess(user)
            }.addOnFailureListener { exception ->
                val abcException = when (exception) {
                    is ApiException -> GoogleApiException(exception.statusCode)
                    is FirebaseAuthInvalidUserException -> FirebaseInvalidUserException()
                    is FirebaseAuthInvalidCredentialsException -> FirebaseInvalidCredentialException()
                    is FirebaseAuthUserCollisionException -> FirebaseUserCollisionException()
                    else -> exception
                }
                firebaseAuthSingle.onError(abcException)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        disposal = TedRx2Permission.with(this)
                .setRationaleTitle(getString(R.string.dialog_title_permission_request))
                .setRationaleMessage(getString(R.string.dialog_message_permission_request))
                .setPermissions(*abc.getAllRequiredPermissions().toTypedArray())
                .request().flatMap { result ->
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
                        Log.d(TAG, "isWhitelested")
                        Single.just<Boolean>(true)
                    } else {
                        Log.d(TAG, "testWhitelist")
                        requestWhiteList()

                        whiteListSingle
                    }
                }.flatMap { isWhiteListed ->
                    if (!isWhiteListed) throw WhiteListDeniedException()
                    requestGoogleSignIn()

                    return@flatMap googleSignInSingle
                }.flatMap { intent ->
                    requestFirebaseAuthorize(intent)

                    return@flatMap firebaseAuthSingle
                }.subscribe { user, exception ->
                    if (user != null) {
                        val message = listOf(
                                getString(R.string.msg_sign_in_success), user.displayName
                                ?: user.email ?: ""
                        ).joinToString(separator = " ")
                        showToast(message, true)
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        val message = when (exception) {
                            is GoogleApiException -> listOfNotNull(
                                    getString(exception.stringRes), exception.message
                            ).joinToString(separator = ": ")
                            is ABCException -> getString(exception.stringRes)
                            else -> getString(R.string.error_general)
                        }
                        showToast(message, true)
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