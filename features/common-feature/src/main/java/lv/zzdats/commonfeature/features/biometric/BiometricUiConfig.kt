// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.biometric

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.serializer.UiSerializable
import lv.zzdats.uilogic.serializer.UiSerializableParser
import lv.zzdats.uilogic.serializer.adapter.SerializableTypeAdapter

data class BiometricUiConfig(
    val title: String,
    val subTitle: String,
    val quickPinOnlySubTitle: String,
    val isPreAuthorization: Boolean = false,
    val shouldInitializeBiometricAuthOnCreate: Boolean = true,
    val onSuccessNavigation: ConfigNavigation,
    val onBackNavigationConfig: OnBackNavigationConfig
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "biometricConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}

data class OnBackNavigationConfig(
    val onBackNavigation: ConfigNavigation?,
    private val hasToolbarCancelIcon: Boolean
) {
    val isCancellable: Boolean get() = hasToolbarCancelIcon && onBackNavigation != null
}