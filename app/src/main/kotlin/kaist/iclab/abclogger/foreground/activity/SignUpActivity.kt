package kaist.iclab.abclogger.foreground.activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.*
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.InvalidContentException
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.prefs


class SignUpActivity : BaseAppCompatActivity() {
    private lateinit var loadStateLiveData: MutableLiveData<LoadState>
    private lateinit var emailWatcher: ErrorWatcher
    private lateinit var passwordWatcher: ErrorWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupContentView()
        setupActionBar()
        setupObservers()
        setupListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }


    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
        container.addView(LayoutInflater.from(this).inflate(R.layout.activity_sign_up, container, false))

        emailWatcher = ErrorWatcher(edtEmail, getString(R.string.edt_error_email_invalid)) {
            FormatUtils.validateEmail(it)
        }
        passwordWatcher = ErrorWatcher(edtPassword, getString(R.string.edt_error_password_invalid)) {
            FormatUtils.validateTextLength(it, 8)
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = getString(R.string.activity_title_sign_up)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupObservers() {
        loadStateLiveData = MutableLiveData()

        loadStateLiveData.observe(this, Observer {
            edtEmail.isEnabled = it?.status != LoadStatus.RUNNING
            edtPassword.isErrorEnabled = it?.status != LoadStatus.RUNNING
            when(it?.status) {
                LoadStatus.RUNNING -> btnSignUp.startWith ()
                LoadStatus.SUCCESS -> btnSignUp.succeedWith(false) {
                    ViewUtils.showToast(this, R.string.msg_auth_request_verify_email)
                    finish()
                }
                LoadStatus.FAILED -> btnSignUp.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
                            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_email
                            is FirebaseAuthUserCollisionException -> R.string.error_user_already_exists
                            is ABCException -> it.error.getErrorStringRes()
                            else -> R.string.error_general_error
                        }
                    )
                }
                else -> { }
            }
        })
    }

    private fun setupListeners() {
        edtEmail.editText?.addTextChangedListener(emailWatcher)
        edtPassword.editText?.addTextChangedListener(passwordWatcher)

        btnSignUp.setOnClickListener {
            loadStateLiveData.postValue(LoadState.LOADING)

            val executor = Executors.newSingleThreadExecutor()
            Tasks.call(executor, Callable {
                if(!FormatUtils.validateEmail(edtEmail) || !FormatUtils.validateTextLength(edtPassword, 10)) throw InvalidContentException()

                val email = edtEmail.editText?.text?.toString()!!
                val phoneNumber = edtPassword.editText?.text?.toString()!!
                Pair(email, phoneNumber)
            }).addOnSuccessListener { result ->
                val email = result?.first!!
                val phoneNumber = result.second

                prefs.participantSignedUp = true
                prefs.participantEmail = email
                prefs.participantPhoneNumber = phoneNumber
                if (switchGroup.isChecked) {
                    prefs.participantGroup = "suggest"
                } else {
                    prefs.participantGroup = "focus more"
                }

                //*
                val entity = ParticipationEntity(
                        experimentUuid = prefs.participantGroup!!,
                        experimentGroup = prefs.participantGroup!!,
                        subjectEmail = prefs.participantEmail!!,
                        subjectPhoneNumber = prefs.participantPhoneNumber!!
                )
                App.boxFor<ParticipationEntity>().put(entity)
                //*/

                Log.d(TAG, "participation prefs input")
                loadStateLiveData.postValue(LoadState.LOADED)
            }.addOnFailureListener {exception ->
                loadStateLiveData.postValue(LoadState.ERROR(exception))
            }

            /*
            Tasks.call(executor, Callable {
                if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if(!FormatUtils.validateEmail(edtEmail) || !FormatUtils.validateTextLength(edtPassword, 8)) throw InvalidContentException()

                /* network/email verification 이후에 user info 활용. Local data 로? */
                val email = edtEmail.editText?.text?.toString()!!
                val password = edtPassword.editText?.text?.toString()!!
                Pair(email, password)
            }).onSuccessTask(executor, SuccessContinuation<Pair<String, String>, AuthResult> {result ->
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                val email = result?.first!!
                val password = result.second

                auth.createUserWithEmailAndPassword(email, password)
            }).onSuccessTask(executor, SuccessContinuation<AuthResult, Void> { result ->
                result?.user?.sendEmailVerification()!!
            }).addOnSuccessListener {_ ->
                loadStateLiveData.postValue(LoadState.LOADED)
            }.addOnFailureListener { exception ->
                loadStateLiveData.postValue(LoadState.ERROR(exception))
            }
            */
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, SignUpActivity::class.java)
    }
}