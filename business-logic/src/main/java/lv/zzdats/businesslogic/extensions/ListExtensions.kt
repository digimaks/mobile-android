// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

fun <T> MutableList<T>.addOrReplace(value: T, replaceCondition: (T) -> Boolean) {
    for (i in indices) {
        if (replaceCondition(this[i])) {
            this[i] = value
        }
    }
}