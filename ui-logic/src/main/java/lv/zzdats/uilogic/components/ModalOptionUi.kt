// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components

import lv.zzdats.uilogic.mvi.ViewEvent

data class ModalOptionUi<T : ViewEvent>(
    val title: String,
    val icon: IconData,
    val event: T
)