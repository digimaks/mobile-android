// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val identifier: String
)