// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.model

data class OnboardingState(
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val email: String? = null,
    val phone: String? = null,
    val registrationId: String? = null
)