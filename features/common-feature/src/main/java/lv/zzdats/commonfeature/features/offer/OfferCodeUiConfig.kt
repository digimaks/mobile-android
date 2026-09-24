// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.offer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.serializer.UiSerializable
import lv.zzdats.uilogic.serializer.UiSerializableParser
import lv.zzdats.uilogic.serializer.adapter.SerializableTypeAdapter

data class OfferCodeUiConfig(
    val offerURI: String,
    val txCodeLength: Int,
    val issuerName: String,
    val onSuccessNavigation: ConfigNavigation
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "offerCodeUiConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}