// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun JSONObject.getStringFromJsonOrEmpty(key: String): String {
    return try {
        this.getString(key)
    } catch (e: JSONException) {
        ""
    }
}
fun JSONArray.toList(): List<Any> {
    return (0 until this.length()).map {
        this.get(it)
    }
}