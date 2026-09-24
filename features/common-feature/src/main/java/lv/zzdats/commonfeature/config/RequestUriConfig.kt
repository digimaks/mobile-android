// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.zzdats.corelogic.controller.PresentationControllerConfig
import lv.zzdats.uilogic.serializer.UiSerializable
import lv.zzdats.uilogic.serializer.UiSerializableParser
import lv.zzdats.uilogic.serializer.adapter.SerializableTypeAdapter

sealed interface PresentationMode {
    data class OpenId4Vp(val uri: String, val initiatorRoute: String) : PresentationMode
    data class Ble(val initiatorRoute: String) : PresentationMode
}

data class RequestUriConfig(
    val mode: PresentationMode
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "requestUriConfig"

        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                PresentationMode::class.java,
                SerializableTypeAdapter<PresentationMode>()
            ).create()
        }
    }
}

fun RequestUriConfig.toDomainConfig(): PresentationControllerConfig {
    return when (mode) {
        is PresentationMode.Ble -> PresentationControllerConfig.Ble(mode.initiatorRoute)
        is PresentationMode.OpenId4Vp -> PresentationControllerConfig.OpenId4VP(
            mode.uri,
            mode.initiatorRoute
        )
    }
}