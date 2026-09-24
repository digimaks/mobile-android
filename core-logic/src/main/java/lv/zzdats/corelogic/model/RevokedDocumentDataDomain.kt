// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RevokedDocumentDataDomain(
    val name: String,
    val id: String,
) : Parcelable
