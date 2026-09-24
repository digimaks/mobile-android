// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.config

import lv.zzdats.authlogic.provider.BiometryStorageProvider
import lv.zzdats.authlogic.provider.OnboardingStorageProvider
import lv.zzdats.authlogic.provider.PinStorageProvider

interface StorageConfig {
    val pinStorageProvider: PinStorageProvider
    val biometryStorageProvider: BiometryStorageProvider
    val onboardingStorageProvider: OnboardingStorageProvider
}

class StorageConfigImpl(
    private val pinImpl: PinStorageProvider,
    private val biometryImpl: BiometryStorageProvider,
    private val onboardingImpl: OnboardingStorageProvider
) : StorageConfig {
    override val pinStorageProvider: PinStorageProvider
        get() = pinImpl
    override val biometryStorageProvider: BiometryStorageProvider
        get() = biometryImpl
    override val onboardingStorageProvider: OnboardingStorageProvider
        get() = onboardingImpl
}