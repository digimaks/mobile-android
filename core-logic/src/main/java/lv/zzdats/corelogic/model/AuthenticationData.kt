// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.model

import lv.zzdats.authlogic.model.BiometricCrypto

data class AuthenticationData(
    val crypto: BiometricCrypto,
    val onAuthenticationSuccess: () -> Unit
)