// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.session

data class SessionResponse(
    val token: String
)

data class SessionStatus(
    val active: Boolean,
    val secondsToLive: Int,
    val secondsToCountdown: Int
)