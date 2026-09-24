// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.qr_scan

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.zzdats.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.zzdats.uilogic.serializer.UiSerializable
import lv.zzdats.uilogic.serializer.UiSerializableParser
import lv.zzdats.uilogic.serializer.adapter.SerializableTypeAdapter

sealed interface QrScanFlow {
    data object Presentation : QrScanFlow
    data class Issuance(val issuanceFlow: IssuanceFlowUiConfig) : QrScanFlow
}

data class QrScanUiConfig(
    val title: String,
    val subTitle: String,
    val qrScanFlow: QrScanFlow
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "qrScanConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                QrScanFlow::class.java,
                SerializableTypeAdapter<QrScanFlow>()
            ).create()
        }
    }
}