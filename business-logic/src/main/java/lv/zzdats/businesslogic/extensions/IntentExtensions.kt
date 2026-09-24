// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.annotation.CheckResult

@CheckResult
inline fun <reified T : Parcelable> Intent?.getParcelableArrayListExtra(
    action: String
): List<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this?.getParcelableArrayListExtra(
            action,
            T::class.java
        )
    } else {
        @Suppress("DEPRECATION")
        this?.getParcelableArrayListExtra(action)
    }?.filterNotNull()
}