package kaist.iclab.abclogger.ui.config

import android.app.PendingIntent
import android.content.Context
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.navArgs
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.structure.config.Config
import kaist.iclab.abclogger.ui.State
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.viewmodel.ext.android.stateSharedViewModel


class ConfigCollectorFragment : ConfigFragment() {
    private val args: ConfigCollectorFragmentArgs by navArgs()
    override val viewModel: ConfigViewModel by stateSharedViewModel()
    override val config: Flow<Config> by lazy { viewModel.getCollectorConfig(args.qualifiedName) }

    override fun initView(viewBinding: FragmentConfigBinding) {
        requireActivity().title = args.name
        requireActivity().actionBar?.title = args.name
        requireActivity().actionBar?.subtitle = args.description

        super.initView(viewBinding)
    }

    companion object {
        fun pendingIntent(
            context: Context,
            qualifiedName: String,
            name: String,
            description: String
        ): PendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main)
            .setDestination(R.id.config_collector)
            .setArguments(
                ConfigCollectorFragmentArgs(
                    qualifiedName = qualifiedName,
                    name = name,
                    description = description
                ).toBundle()
            )
            .createPendingIntent()
    }
}