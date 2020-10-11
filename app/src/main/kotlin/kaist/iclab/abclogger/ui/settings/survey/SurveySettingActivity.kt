package kaist.iclab.abclogger.ui.settings.survey

import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.settings.AbstractSettingActivity
import kaist.iclab.abclogger.structure.survey.Survey
import kaist.iclab.abclogger.structure.survey.SurveyConfiguration
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.LayoutSettingSurveyBinding
import kaist.iclab.abclogger.dialog.ChoiceDialog
import kaist.iclab.abclogger.dialog.VersatileDialog
import kaist.iclab.abclogger.ui.State
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class SurveySettingActivity :
    AbstractSettingActivity<LayoutSettingSurveyBinding, SurveySettingViewModel>(),
    SurveyConfigurationListAdapter.OnItemClickListener {
    override val viewModel: SurveySettingViewModel by stateViewModel()

    override fun getInnerViewBinding(inflater: LayoutInflater): LayoutSettingSurveyBinding =
        LayoutSettingSurveyBinding.inflate(inflater)

    private val adapter = SurveyConfigurationListAdapter()

    override fun afterToolbarCreated() {
        adapter.setOnItemClickListener(this)

        childBinding.recyclerView.adapter = adapter
        childBinding.recyclerView.itemAnimator = DefaultItemAnimator()

        childBinding.containerBaseDate.setOnClickListener { onClickBaseDate() }
        childBinding.fabAddSurvey.setOnClickListener { onClickAddSurvey() }

        lifecycleScope.launchWhenCreated {
            val configurations = viewModel.getConfigurations()
            val baseScheduleDate = viewModel.baseScheduledDate

            updateUi(baseScheduleDate)
            setConfigurations(configurations)

            viewModel.saveState(KEY_BASE_SCHEDULE_DATE, baseScheduleDate)
            viewModel.saveState(KEY_CONFIGURATIONS, arrayListOf(*configurations.toTypedArray()))
        }
    }

    override fun undo() {
        lifecycleScope.launchWhenCreated {
            val configurations: Array<SurveyConfiguration> = viewModel.loadState(KEY_CONFIGURATIONS) ?: arrayOf()
            val baseScheduleDate: Long = viewModel.loadState(KEY_BASE_SCHEDULE_DATE) ?: Long.MIN_VALUE

            viewModel.baseScheduledDate = baseScheduleDate
            viewModel.setConfigurations(configurations.toList())

            updateUi(baseScheduleDate)
            setConfigurations(configurations.toList())
        }
    }

    private fun onClickBaseDate() {
        lifecycleScope.launchWhenCreated {
            val choice = ChoiceDialog.show(
                manager = supportFragmentManager,
                owner = this@SurveySettingActivity,
                texts = arrayOf(
                    getString(R.string.setting_survey_base_date_option_change),
                    getString(R.string.setting_survey_base_date_option_clear)
                ),
                icons = intArrayOf(
                    R.drawable.baseline_date_range_24,
                    R.drawable.baseline_clear_24
                )
            )

            when (choice) {
                0 -> {
                    val baseScheduleDate = VersatileDialog.date(
                        manager = supportFragmentManager,
                        owner = this@SurveySettingActivity,
                        title = getString(R.string.setting_survey_base_date_dialog_change),
                        value = viewModel.baseScheduledDate.takeIf { it > 0 }
                    )

                    if (baseScheduleDate != null) {
                        viewModel.baseScheduledDate = baseScheduleDate
                        updateUi(baseScheduleDate)
                    }
                }
                1 -> viewModel.baseScheduledDate = Long.MIN_VALUE
            }
        }
    }

    private fun onClickAddSurvey() {
        lifecycleScope.launchWhenCreated {
            val url = openUrlDialog(null)

            if (url != null) {
                addConfiguration(SurveyConfiguration(url = url))
            }
        }
    }

    private fun setConfigurations(config: List<SurveyConfiguration>) {
        adapter.setItems(config)
        updateUi()
    }

    private fun addConfiguration(config: SurveyConfiguration) {
        adapter.addItem(config)
        updateUi()
        lifecycleScope.launchWhenCreated {
            download(config)
            viewModel.setConfigurations(adapter.getItems())
        }
    }

    private fun removeConfiguration(config: SurveyConfiguration) {
        adapter.removeItem(config)
        updateUi()

        lifecycleScope.launchWhenCreated {
            viewModel.setConfigurations(adapter.getItems())
        }
    }

    private fun changeConfiguration(prevConfig: SurveyConfiguration, newConfig: SurveyConfiguration) {
        adapter.changeItem(prevConfig, newConfig)
        updateUi()

        lifecycleScope.launchWhenCreated {
            download(newConfig)
            viewModel.setConfigurations(adapter.getItems())
        }
    }

    override fun onItemClick(position: Int, item: SurveyConfiguration) {
        lifecycleScope.launchWhenCreated {
            val choice = ChoiceDialog.show(
                manager = supportFragmentManager,
                owner = this@SurveySettingActivity,
                texts = arrayOf(
                    getString(R.string.setting_survey_config_option_preview),
                    getString(R.string.setting_survey_config_option_change),
                    getString(R.string.setting_survey_config_option_open),
                    getString(R.string.setting_survey_config_option_remove)
                ),
                icons = intArrayOf(
                    R.drawable.baseline_preview_24,
                    R.drawable.baseline_edit_24,
                    R.drawable.baseline_open_in_browser_24,
                    R.drawable.baseline_clear_24
                )
            )

            when (choice) {
                0 -> SurveyPreviewDialogFragment.newInstance(item.survey)
                    .show(supportFragmentManager, null)
                1 -> openUrlDialog(item.url)?.let { changeConfiguration(item, item.copy(url = it)) }
                2 -> try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                } catch (e: Exception) {
                    showSnackBar(
                        viewBinding.root,
                        AbcError.wrap(e).toSimpleString(this@SurveySettingActivity)
                    )
                }
                3 -> removeConfiguration(item)
            }
        }
    }

    private suspend fun openUrlDialog(url: String?) = VersatileDialog.text(
        manager = supportFragmentManager,
        owner = this@SurveySettingActivity,
        title = getString(R.string.setting_survey_config_dialog_add),
        value = url,
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
    )

    private suspend fun download(item: SurveyConfiguration) {
        viewModel.download(item.url).collectLatest { state ->
            item.isLoading = state == State.Loading

            when (state) {
                is State.Success<*> -> {
                    adapter.changeItem(
                        item,
                        item.copy(
                            survey = (state.data as? Survey) ?: Survey.Empty,
                            lastAccessTime = System.currentTimeMillis()
                        )
                    )
                }
                is State.Failure -> item.apply {
                    item.error =
                        AbcError.wrap(state.error).toSimpleString(this@SurveySettingActivity)
                }
            }
        }
    }

    private fun updateUi(baseScheduleDate: Long) {
        childBinding.txtBaseDateText.text =
            baseScheduleDate.takeIf { it > 0 }?.let { Formatter.formatDate(this, baseScheduleDate) }
    }

    private fun updateUi() {
        childBinding.recyclerView.visibility =
            if (adapter.itemCount > 0) View.VISIBLE else View.GONE
        childBinding.txtNoSurvey.visibility = if (adapter.itemCount > 0) View.GONE else View.VISIBLE
    }

    companion object {
        private const val KEY_BASE_SCHEDULE_DATE = "KEY_BASE_SCHEDULE_DATE"
        private const val KEY_CONFIGURATIONS = "KEY_CONFIGURATIONS"
    }


}