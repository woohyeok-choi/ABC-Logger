package kaist.iclab.abclogger.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.iid.FirebaseInstanceId
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.GoogleApiError
import kaist.iclab.abclogger.commons.PreconditionError
import kaist.iclab.abclogger.commons.toCoroutine

object AuthRepository {
    private fun getSignInOption(context: Context) = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

    fun email(): String = FirebaseAuth.getInstance().currentUser?.email.takeUnless { it.isNullOrBlank() } ?: Preference.lastSignedEmail

    fun name(): String = FirebaseAuth.getInstance().currentUser?.displayName.takeUnless { it.isNullOrBlank() } ?: Preference.lastSignedName

    fun isSignedIn() = FirebaseAuth.getInstance().currentUser != null

    fun instanceId() : String = FirebaseInstanceId.getInstance().id

    val deviceManufacturer = Build.MANUFACTURER

    val deviceModel = Build.MODEL

    val deviceVersion = Build.VERSION.RELEASE

    val deviceOs = "Android-${Build.VERSION.SDK_INT}"

    val appVersion = BuildConfig.VERSION_NAME

    val source: String = "SMARTPHONE"

    var groupName: String
        get() = Preference.groupName
        set(value) {
            Preference.groupName = value
        }

    val appId: String = BuildConfig.APPLICATION_ID

    fun avatarUrl(context: Context): String? = GoogleSignIn.getLastSignedInAccount(context)?.photoUrl?.toString()

    suspend fun updateGooglePlayService(activity: Activity) {
        GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(activity)
                .toCoroutine(throwable = PreconditionError.googlePlayServiceOutdated())
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val client = GoogleSignIn.getClient(context, getSignInOption(context))
        return client.signInIntent
    }

    suspend fun authorizeFirebaseWithGoogleAccount(intent: Intent?) : FirebaseUser? {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent).toCoroutine() ?: throw GoogleApiError.noSignedAccount()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val user = FirebaseAuth.getInstance().signInWithCredential(credential).toCoroutine()?.user

        Preference.lastSignedEmail = user?.email ?: ""
        Preference.lastSignedName = user?.displayName ?: ""

        return user
    }

    suspend fun signOut(context: Context) {
        GoogleSignIn.getClient(context, getSignInOption(context)).signOut().toCoroutine()
        FirebaseAuth.getInstance().signOut()
    }
}