// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.controller.storage

import lv.zzdats.authlogic.config.StorageConfig
import lv.zzdats.authlogic.model.BiometricAuth

interface BiometryStorageController {
    fun getBiometricAuth(): BiometricAuth?
    fun setBiometricAuth(value: BiometricAuth?)
    fun setUseBiometricsAuth(value: Boolean)
    fun getUseBiometricsAuth(): Boolean
}

class BiometryStorageControllerImpl(private val storageConfig: StorageConfig) :
    BiometryStorageController {
    override fun getBiometricAuth(): BiometricAuth? =
        storageConfig.biometryStorageProvider.getBiometricAuth()

    override fun setBiometricAuth(value: BiometricAuth?) {
        storageConfig.biometryStorageProvider.setBiometricAuth(value)
    }

    override fun setUseBiometricsAuth(value: Boolean) {
        storageConfig.biometryStorageProvider.setUseBiometricsAuth(value)
    }

    override fun getUseBiometricsAuth(): Boolean =
        storageConfig.biometryStorageProvider.getUseBiometricsAuth()
}