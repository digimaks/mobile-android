// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.provider

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface UuidProvider {
    fun provideUuid(): String
}

class UuidProviderImpl() : UuidProvider {
    @OptIn(ExperimentalUuidApi::class)
    override fun provideUuid(): String {
        return Uuid.random().toString()
    }
}