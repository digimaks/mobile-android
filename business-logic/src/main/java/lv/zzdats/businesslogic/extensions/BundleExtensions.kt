// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

import android.os.Bundle

fun Bundle?.toMapOrEmpty(): Map<String, String> {
    return this?.let { bundle ->
        mutableMapOf<String, String>().apply {
            bundle.keySet().forEach {
                bundle.getString(it)?.let { value ->
                    put(it, value)
                }
            }
        }
    } ?: emptyMap()
}