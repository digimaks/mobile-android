// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.analyticslogic.provider

import android.app.Application

interface AnalyticsProvider {
    fun initialize(context: Application, key: String)
    fun logScreen(name: String, arguments: Map<String, String> = emptyMap())
    fun logEvent(event: String, arguments: Map<String, String>)
    fun setCustomKeys(keys: Map<String, String>)
}
