package kaist.iclab.abclogger.ui.survey.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.ItemSurveyListLoadStateBinding

class SurveyLoadStateAdapter : LoadStateAdapter<SurveyLoadStateAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder =
            ViewHolder(
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_survey_list_load_state,
                            parent,
                            false
                    )
            )

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) = holder.onBind(loadState)

    data class ViewHolder(private val binding: ItemSurveyListLoadStateBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind(loadState: LoadState) {
            binding.isLoading = loadState == LoadState.Loading
        }
    }
}