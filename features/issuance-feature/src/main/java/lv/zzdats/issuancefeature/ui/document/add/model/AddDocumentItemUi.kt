// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.ui.document.add.model

import lv.zzdats.uilogic.components.IconData

data class DocumentOptionItemUi(
    val configId: String,
    val text: String,
    val icon: IconData,
    val available: Boolean,
    val alreadyHave: Boolean
)