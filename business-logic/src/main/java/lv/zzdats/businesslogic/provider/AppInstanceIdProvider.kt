// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.provider

import lv.zzdats.businesslogic.controller.PrefKeys

interface AppInstanceIdProvider {
    fun getAppInstanceId(): String
}

class AppInstanceIdProviderImpl(
    private val prefKeys: PrefKeys,
    private val uuidProvider: UuidProvider
) : AppInstanceIdProvider {

    @Synchronized
    override fun getAppInstanceId(): String {
        val existing = prefKeys.getAppInstanceId()
        if (existing.isNotBlank()) {
            return existing
        }

        return uuidProvider.provideUuid().also(prefKeys::setAppInstanceId)
    }
}
