package kaist.iclab.abclogger.foreground.activity

import android.app.Activity
import android.app.ActivityManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.AsyncQueryHandler
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.background.SyncManager
import kaist.iclab.abclogger.common.*
import kaist.iclab.abclogger.common.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.data.DataProvider
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.exportSQLite
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.forceToWriteContentValues
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.prefs
import kaist.iclab.abclogger.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.activity_container_without_toolbar.*
import kotlinx.android.synthetic.main.activity_sign_in.*
import java.io.File


class SignInActivity : BaseAppCompatActivity() {
    private lateinit var loadState: MutableLiveData<LoadState>
    private lateinit var emailWatcher: ErrorWatcher
    private lateinit var passwordWatcher: ErrorWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupObservers()
        setupListener()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if(intent?.hasExtra(EXTRA_SIGN_IN_EMAIL) == true) {
            edtEmail.editText?.setText( intent.getStringExtra(EXTRA_SIGN_IN_EMAIL) ?: "", TextView.BufferType.EDITABLE)
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_without_toolbar)
        container.addView(layoutInflater.inflate(R.layout.activity_sign_in, container, false))

        emailWatcher = ErrorWatcher(edtEmail, getString(R.string.edt_error_email_invalid)) {
            FormatUtils.validateEmail(it)
        }
        passwordWatcher = ErrorWatcher(edtPassword, getString(R.string.edt_error_password_invalid)) {
            FormatUtils.validateTextLength(it, 8)
        }
    }


    private fun setupObservers() {
        loadState = MutableLiveData()

        loadState.observe(this, Observer {
            edtEmail.isEnabled = it?.status != LoadStatus.RUNNING
            edtPassword.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> btnSignIn.startWith()
                LoadStatus.SUCCESS -> btnSignIn.succeedWith(false) {
                    startActivity(
                        MainActivity.newIntent(this, true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
                LoadStatus.FAILED -> btnSignIn.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthInvalidUserException -> R.string.error_no_account
                            is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_password_incorrect
                            is ABCException -> it.error.getErrorStringRes()
                            else -> R.string.error_general_error
                        })
                }
                else -> { }
            }
        })
    }

    private fun setupListener() {
        edtEmail.editText?.addTextChangedListener(emailWatcher)
        edtPassword.editText?.addTextChangedListener(passwordWatcher)

        btnCreateAccount.setOnClickListener {
            startActivity(SignUpActivity.newIntent(this))
        }

        btnForgetPassword.setOnClickListener {
            //startActivity(PasswordResetActivity.newIntent(this))
            //startActivity(AppUsageCollector.newIntentForSetup())
            //ABCPlatform.stop(this)

            /*
            val values = ContentValues()
            val json = JSONObject()
            json.put("abcLogger", "test")
            values.put(DAO.LOG_FIELD_TYPE, "")
            values.put(DAO.LOG_FIELD_REG, System.currentTimeMillis())
            values.put(DAO.LOG_FIELD_JSON, "")
            Log.d(TAG, "${values.get(DAO.LOG_FIELD_TYPE)} + ${values.get(DAO.LOG_FIELD_REG)}" +
                    " + ${values.get(DAO.LOG_FIELD_JSON)}")
            val handler = object : AsyncQueryHandler(contentResolver) {}

            handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
            */

            //writeStringData(applicationContext,"abcLogger", System.currentTimeMillis(), "test")
            /*
            val am = applicationContext.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager

            am.getRunningServices(Integer.MAX_VALUE).forEach {
                //Log.i(TAG, it.javaClass.simpleName.toString())
                Log.i(TAG, "process: ${it.process}, " +
                        "clientPackage: ${it.clientPackage}, " +
                        "service className: ${it.service.className}, " +
                        "service shortClassName: ${it.service.shortClassName}, " +
                        "service packageName: ${it.service.packageName}")
            }
            */
        }

        btnSignIn.setOnClickListener {
            /*
            아마 여기서 조작하면 될듯!
             */

            val email = edtEmail.editText?.text?.toString()
            val phoneNumber = edtPassword.editText?.text?.toString()

            loadState.postValue(LoadState.LOADING)

            /* 강제로 DB 업로드 하는 기능임. */
            if (email.equals("sw.kang@kaist.ac.kr") &&
                    phoneNumber.equals("01046273313")) {
                exportSQLite(applicationContext, prefs.participantPhoneNumber)
                Toast.makeText(applicationContext, "FORCED UPDATE", Toast.LENGTH_SHORT).show()
            }

            if (prefs.participantSignedUp) {
                if (email.equals(prefs.participantEmail) &&
                    phoneNumber.equals(prefs.participantPhoneNumber)) {
                    val entity = ParticipationEntity.getParticipationFromLocal()
                    val jsonString = getString(R.string.esm)
                    val box = App.boxFor<ParticipationEntity>()
                    box.put(entity.copy(
                            subjectPhoneNumber = prefs.participantPhoneNumber!!,
                            subjectEmail = prefs.participantEmail!!,
                            participateTime = System.currentTimeMillis(),
                            survey = jsonString
                    ))
                    prefs.lastTimeSurveyTriggered = Long.MIN_VALUE
                    loadState.postValue(LoadState.LOADED)
                } else {
                    Toast.makeText(this, "Account not verified", Toast.LENGTH_LONG).show()
                    loadState.postValue(LoadState.ERROR(NotVerifiedAccountException()))
                }
            } else {
                Toast.makeText(this, "Account not exists", Toast.LENGTH_LONG).show()
                loadState.postValue(LoadState.ERROR(NoSignedAccountException()))
            }

            //ABCPlatform.start(this)

            /*
            val participation = ParticipationEntity.getParticipationFromLocal()
            val uuid = participation.subjectPhoneNumber
            val group = participation.experimentGroup
            */
            /*
            val postMsg = PhpApi.dataToByteArray(uuid, group,false, false, false)
            val result = PhpApi.request(this, "http://143.248.90.57/server.php", postMsg)

            loadState.postValue(LoadState.LOADING)
            result.addOnSuccessListener {
                Log.d(TAG, "success: $it")
                loadState.postValue(LoadState.LOADED)
            }.addOnFailureListener {
                loadState.postValue(LoadState.ERROR(it))
            }
            */


            /*
            loadState.postValue(LoadState.LOADING)

            val executor = Executors.newSingleThreadExecutor()

            Tasks.call(executor, Callable {
                if (!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if (!FormatUtils.validateEmail(edtEmail) || !FormatUtils.validateTextLength(edtPassword, 8)) throw InvalidContentException()
                val email = edtEmail.editText?.text?.toString()!!
                val password = edtPassword.editText?.text?.toString()!!
                Pair(email, password)
            }).onSuccessTask(executor, SuccessContinuation<Pair<String, String>, AuthResult> { result ->
                val email = result?.first!!
                val password = result.second
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                auth.signInWithEmailAndPassword(email, password)
            }).onSuccessTask(executor, SuccessContinuation<AuthResult, FirestoreAccessor.SubjectData> { result ->
                val user = result?.user ?: throw NoSignedAccountException()
                if (!user.isEmailVerified) throw NotVerifiedAccountException()
                FirestoreAccessor.get(user.email!!)
            }).onSuccessTask(executor, SuccessContinuation<FirestoreAccessor.SubjectData, ParticipationEntity?> { result ->
                //if(result?.uuid == PreferenceAccessor.getInstance(this).deviceUuid) throw AlreadySignedInAccountException()
                Tasks.call {
                    try {
                    ParticipationEntity.getParticipatedExperimentFromServer(this)
                    } catch (e: Exception) {
                        null
                    }
                }
            }).addOnSuccessListener { _ ->
                loadState.postValue(LoadState.LOADED)
            }.addOnFailureListener {exception ->
                if(exception is AlreadySignedInAccountException) {
                    val title = getString(R.string.dialog_title_already_sign_in)
                    val message = getString(exception.getErrorStringRes())
                    val dialog = YesNoDialogFragment.newInstance(title, message)
                    dialog.setOnDialogOptionSelectedListener { result ->
                        if (result) {
                            loadState.postValue(LoadState.LOADED)
                        } else {
                            loadState.postValue(LoadState.ERROR(SignInCanceledException()))
                        }
                    }
                    dialog.show(supportFragmentManager, TAG)
                } else {
                    loadState.postValue(LoadState.ERROR(exception))
                }
            }
            */
        }
    }

    companion object {
        private val EXTRA_SIGN_IN_EMAIL = "${SignInActivity::class.java.canonicalName}.EXTRA_SIGN_IN_EMAIL"

        fun newIntent(context: Context, email: String? = null) : Intent = Intent(context, SignInActivity::class.java).apply {
            if(!TextUtils.isEmpty(email)) putExtra(EXTRA_SIGN_IN_EMAIL, email)
        }
    }

}