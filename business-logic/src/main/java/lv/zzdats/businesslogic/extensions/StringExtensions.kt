// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

import android.util.Base64

fun String.decodeFromPemBase64String(): ByteArray? {
    return Base64.decode(this.replace("\n", ""), Base64.NO_WRAP)
}

fun String.encodeToBase64(): String = Base64.encodeToString(
    this.toByteArray(Charsets.UTF_8),
    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
)

fun String.decodeFromBase64(): String = Base64.decode(
    this, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
).toString(Charsets.UTF_8)

fun String.splitToLines(lineLength: Int): String {
    return if (lineLength <= 0 || this.length <= lineLength) this
    else {
        var result = ""
        var index = 0
        while (index < length) {
            val line = substring(index, (index + lineLength).coerceAtMost(length))
            result += (if (result.isEmpty()) line else "\n$line")
            index += lineLength
        }
        result
    }
}

fun String.firstPart(separator: String): String = this.split(separator).firstOrNull() ?: this

