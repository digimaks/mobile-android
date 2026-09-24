// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.serializer

import com.google.gson.Gson

interface UiSerializable

interface UiSerializableParser {
    val serializedKeyName: String
    fun provideParser(): Gson {
        return Gson()
    }
}