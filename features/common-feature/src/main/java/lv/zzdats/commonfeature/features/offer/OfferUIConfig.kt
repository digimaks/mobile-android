// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.offer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.serializer.UiSerializable
import lv.zzdats.uilogic.serializer.UiSerializableParser
import lv.zzdats.uilogic.serializer.adapter.SerializableTypeAdapter

data class OfferUiConfig(
    val offerURI: String,
    val onSuccessNavigation: ConfigNavigation,
    val onCancelNavigation: ConfigNavigation,
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "offerConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}