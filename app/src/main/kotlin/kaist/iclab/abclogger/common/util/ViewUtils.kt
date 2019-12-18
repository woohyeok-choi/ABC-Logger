package kaist.iclab.abclogger.common.util

import android.content.Context
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.FragmentManager
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.foreground.dialog.SurveyPreviewDialogFragment
import kaist.iclab.abclogger.foreground.view.*
// import kaist.iclab.abc.protos.ExperimentProtos
import java.util.ArrayList
import kotlin.jvm.java

object ViewUtils {

    fun bindExperimentDescriptionView(view: View, description: String) {
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        txtDescription.text = description
    }
    /*
    fun bindContactView(view: View, experimenter: ExperimentProtos.Experimenter) {
        val txtName: TextView = view.findViewById(R.id.txtExperimenterName)
        val txtEmail: TextView = view.findViewById(R.id.txtExperimenterEmail)
        val txtPhoneNumber: TextView = view.findViewById(R.id.txtExperimenterPhoneNumber)
        val txtWebSite: TextView = view.findViewById(R.id.txtWebSite)

        txtName.text = experimenter.name
        txtEmail.text = experimenter.email
        txtPhoneNumber.text = experimenter.phoneNumber
        txtWebSite.text = experimenter.webSite
    }
    */

    fun bindParticipationInfo(view: View, entity: ParticipationEntity, fragmentManager: androidx.fragment.app.FragmentManager? = null) {
        val itemName: DefaultItemView = view.findViewById(R.id.itemName)
        val itemPhoneNumber: DefaultItemView = view.findViewById(R.id.itemPhoneNumber)
        val itemAffiliation: DefaultItemView = view.findViewById(R.id.itemAffiliation)
        val itemGender: DefaultItemView = view.findViewById(R.id.itemGender)
        val itemBirthDate: DefaultItemView = view.findViewById(R.id.itemBirthDate)
        val itemExperimentGroup: DefaultItemView = view.findViewById(R.id.itemExperimentGroup)
        val itemExperimentSurvey: DefaultItemView = view.findViewById(R.id.itemExperimentSurvey)

        itemName.setDescription(entity.subjectName)
        itemName.setShowMore(false)

        itemPhoneNumber.setDescription(entity.subjectPhoneNumber)
        itemPhoneNumber.setShowMore(false)

        itemAffiliation.setDescription(entity.subjectAffiliation)
        itemAffiliation.setShowMore(false)

        itemGender.setDescription(
            if(entity.subjectIsMale) view.context.getString(R.string.general_gender_male) else view.context.getString(R.string.general_gender_female)
        )
        itemGender.setShowMore(false)

        itemBirthDate.setDescription(entity.subjectBirthDate.toString())
        itemBirthDate.setShowMore(false)

        itemExperimentGroup.setDescription(
            if(TextUtils.isEmpty(entity.experimentGroup)) view.context.getString(R.string.general_undefined) else entity.experimentGroup
        )
        itemExperimentGroup.setShowMore(false)

        if(TextUtils.isEmpty(entity.survey)) {
            itemExperimentSurvey.setDescription(view.context.getString(R.string.general_survey_excluded))
            itemExperimentSurvey.setShowMore(false)
        } else {
            itemExperimentSurvey.setDescription(view.context.getString(R.string.general_survey_included))

            if(fragmentManager != null) {
                itemExperimentSurvey.setShowMore(true)
                itemExperimentSurvey.setOnClickListener {
                    SurveyPreviewDialogFragment.newInstance(null).show(fragmentManager, SurveyView::class.java.name)
                }
            }
        }
    }


    fun bindButton(button: Button, isEnabled: Boolean, stringRes: Int? = null) {
        button.isEnabled = isEnabled
        button.setBackgroundColor(
            ContextCompat.getColor(button.context, if(isEnabled) R.color.colorButton else R.color.colorDisabled)
        )
        if(stringRes != null) {
            button.text = button.context.getString(stringRes)
        }
    }

    fun showSnackBar(view: View, messageRes: Int, showAlways: Boolean = false, actionRes: Int? = null, action: (() -> Unit)? = null) {
        var snackBar = Snackbar.make(view, messageRes, if(showAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
        if(actionRes != null) {
            snackBar = snackBar.setAction(actionRes) {
                action?.invoke()
            }
        }
        snackBar.show()
    }

    fun showToast(context: Context?, messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    fun showPermissionDialog(context: Context, permissions: Array<String>, granted: (() -> Unit)? = null, denied: (() -> Unit)? = null) {
        TedPermission.with(context)
            .setPermissions(*permissions)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    granted?.invoke()
                }

                override fun onPermissionDenied(deniedPermissions: ArrayList<String>?) {
                    denied?.invoke()
                }
            }).check()
    }
}

