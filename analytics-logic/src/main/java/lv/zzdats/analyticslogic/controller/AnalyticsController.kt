// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.analyticslogic.controller

import android.app.Application
import lv.zzdats.analyticslogic.config.AnalyticsConfig

interface AnalyticsController {
    fun initialize(context: Application)
    fun logScreen(name: String, arguments: Map<String, String> = emptyMap())
    fun logEvent(eventName: String, arguments: Map<String, String> = emptyMap())
    fun setCustomKeys(keys: Map<String, String>)
}

class AnalyticsControllerImpl(
    private val analyticsConfig: AnalyticsConfig,
) : AnalyticsController {

    override fun initialize(context: Application) {
        analyticsConfig.analyticsProviders.forEach { (key, analyticProvider) ->
            analyticProvider.initialize(context, key)
        }
    }

    override fun logScreen(name: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logScreen(name, arguments)
        }
    }

    override fun logEvent(eventName: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logEvent(eventName, arguments)
        }
    }

    override fun setCustomKeys(keys: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.setCustomKeys(keys)
        }
    }
}
