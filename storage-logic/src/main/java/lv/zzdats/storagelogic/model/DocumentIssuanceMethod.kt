// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documentIssuanceMethods")
data class DocumentIssuanceMethod(
    @PrimaryKey
    val documentId: String,
    val issuanceMethod: String
)
