// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.analyticslogic.config

import lv.zzdats.analyticslogic.provider.AnalyticsProvider
import lv.zzdats.analyticslogic.provider.FirebaseCrashlyticsProvider

interface AnalyticsConfig {
    val analyticsProviders: Map<String, AnalyticsProvider>
        get() = emptyMap()
}

class AnalyticsConfigImpl : AnalyticsConfig {
    override val analyticsProviders: Map<String, AnalyticsProvider> = mapOf(
        "firebase_crashlytics" to FirebaseCrashlyticsProvider()
    )
}