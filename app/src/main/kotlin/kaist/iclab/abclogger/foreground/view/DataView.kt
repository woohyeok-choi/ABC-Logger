package kaist.iclab.abclogger.foreground.view

import android.content.Context
import androidx.appcompat.widget.LinearLayoutCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import kaist.iclab.abclogger.R

class DataView(context: Context, attributeSet: AttributeSet?) : LinearLayoutCompat(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private val itemDataTrafficAndEvent = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_traffic_and_event))
        setDescription(context.getString(R.string.data_provided_desc_traffic_and_event))
    }

    private var itemDataLocationAndActivity = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_location_and_activity))
        setDescription(context.getString(R.string.data_provided_desc_location_and_activity))
    }

    private var itemDataContentProvider = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_content_provider))
        setDescription(context.getString(R.string.data_provided_desc_content_provider))
    }

    private var itemDataAmbientSound = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_ambient_sound))
        setDescription(context.getString(R.string.data_provided_desc_ambient_sound))
    }

    private var itemDataAppUsage = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_app_usage))
        setDescription(context.getString(R.string.data_provided_desc_app_usage))
    }

    private var itemDataNotification = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_notification))
        setDescription(context.getString(R.string.data_provided_desc_notification))
    }

    private var itemDataGoogleFitness = ProvidedDataItemView(context).apply {
        setHeader(context.getString(R.string.data_provided_title_fitness))
        setDescription(context.getString(R.string.data_provided_desc_fitness))
    }

    init {
        orientation = LinearLayoutCompat.VERTICAL
        layoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)

        addView(itemDataTrafficAndEvent, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataLocationAndActivity, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataContentProvider, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataAmbientSound, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataAppUsage, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataNotification, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
        addView(itemDataGoogleFitness, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT))
    }

    fun setVisibilities(requiresEventAndTraffic: Boolean = true,
                        requiresLocationAndActivity: Boolean = true,
                        requiresContentProviders : Boolean = true,
                        requiresAmbientSound: Boolean = true,
                        requiresAppUsage: Boolean = true,
                        requiresNotification: Boolean = true,
                        requiresGoogleFitness: Boolean = true) {
        itemDataTrafficAndEvent.visibility = if(requiresEventAndTraffic) View.VISIBLE else View.GONE
        itemDataLocationAndActivity.visibility = if(requiresLocationAndActivity) View.VISIBLE else View.GONE
        itemDataContentProvider.visibility = if(requiresContentProviders) View.VISIBLE else View.GONE
        itemDataAmbientSound.visibility = if(requiresAmbientSound) View.VISIBLE else View.GONE
        itemDataAppUsage.visibility = if(requiresAppUsage) View.VISIBLE else View.GONE
        itemDataNotification.visibility = if(requiresNotification) View.VISIBLE else View.GONE
        itemDataGoogleFitness.visibility = if(requiresGoogleFitness) View.VISIBLE else View.GONE
    }

    fun setDescriptions(descEventAndTraffic: String? = null,
                        descLocationAndActivity: String? = null,
                        descContentProviders: String? = null,
                        descAmbientSound: String? = null,
                        descAppUsage: String? = null,
                        descNotification: String? = null,
                        descGoogleFitness: String? = null) {
        if(!TextUtils.isEmpty(descEventAndTraffic)) itemDataTrafficAndEvent.setDescription(descEventAndTraffic)
        if(!TextUtils.isEmpty(descLocationAndActivity)) itemDataLocationAndActivity.setDescription(descLocationAndActivity)
        if(!TextUtils.isEmpty(descContentProviders)) itemDataContentProvider.setDescription(descContentProviders)
        if(!TextUtils.isEmpty(descAmbientSound)) itemDataAmbientSound.setDescription(descAmbientSound)
        if(!TextUtils.isEmpty(descAppUsage)) itemDataAppUsage.setDescription(descAppUsage)
        if(!TextUtils.isEmpty(descNotification)) itemDataNotification.setDescription(descNotification)
        if(!TextUtils.isEmpty(descGoogleFitness)) itemDataGoogleFitness.setDescription(descGoogleFitness)
    }

    fun setShowMore(appUsageBlock: (() -> Unit)? = null, notificationBlock: (() -> Unit)? = null, fitnessBlock: (() -> Unit)? = null) {
        itemDataAppUsage.setShowMore(appUsageBlock != null)
        if(appUsageBlock != null) itemDataAppUsage.setOnClickListener { appUsageBlock() }

        itemDataNotification.setShowMore(notificationBlock != null)
        if(notificationBlock != null) itemDataNotification.setOnClickListener { notificationBlock() }

        itemDataGoogleFitness.setShowMore(fitnessBlock != null)
        if(fitnessBlock != null) itemDataGoogleFitness.setOnClickListener { fitnessBlock() }
    }

    fun setGranted(appUsage: Boolean = true, notification: Boolean = true, fitness: Boolean = true) {
        itemDataAppUsage.setGranted(appUsage)
        itemDataNotification.setGranted(notification)
        itemDataGoogleFitness.setGranted(fitness)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        itemDataTrafficAndEvent.isEnabled = enabled
        itemDataLocationAndActivity.isEnabled = enabled
        itemDataContentProvider.isEnabled = enabled
        itemDataAmbientSound.isEnabled = enabled
        itemDataAppUsage.isEnabled = enabled
        itemDataNotification.isEnabled = enabled
        itemDataGoogleFitness.isEnabled = enabled
    }
}