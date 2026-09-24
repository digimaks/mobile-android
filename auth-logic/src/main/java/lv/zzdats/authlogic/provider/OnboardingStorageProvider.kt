// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.provider

import lv.zzdats.authlogic.model.OnboardingState

interface OnboardingStorageProvider {
    fun getOnboardingState(): OnboardingState
    fun setOnboardingState(state: OnboardingState)
    fun clearOnboardingState()
}