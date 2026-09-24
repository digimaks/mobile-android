// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.user

import com.google.gson.annotations.SerializedName

data class DocumentOfferResponse(
    @SerializedName("urlData") val offerUrl: String
)