package kaist.iclab.abclogger.ui.config

import android.app.PendingIntent
import android.content.Context
import androidx.navigation.NavDeepLinkBuilder
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.structure.config.Config
import kaist.iclab.abclogger.ui.State

import kotlinx.coroutines.flow.Flow
import org.koin.androidx.viewmodel.ext.android.stateSharedViewModel


class ConfigGeneralFragment : ConfigFragment() {
    override val config: Flow<Config> by lazy { viewModel.getConfig() }
    override val viewModel: ConfigViewModel by stateSharedViewModel()

    companion object {
        fun pendingIntent(context: Context): PendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main)
            .setDestination(R.id.config)
            .createPendingIntent()
    }
}