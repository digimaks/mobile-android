// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.config

import com.google.gson.annotations.SerializedName


data class AppVersionConfigResponse(


    @SerializedName("platforms")
    val platforms: Platforms?
)

data class Platforms(
    @SerializedName("android")
    val android: AndroidVersionConfig?
)

data class AndroidVersionConfig(
    @SerializedName("min_required_version")
    val minRequiredVersion: String?,
    @SerializedName("latest_version")
    val latestVersion: String?,
    @SerializedName("store_url")
    val storeUrl: String?
)