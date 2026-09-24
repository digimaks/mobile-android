// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.util

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

class UriCacheManager(private val cachePeriodMillis: Long) {
    private val cache = ConcurrentHashMap<String, Pair<Uri, Long>>() // key -> (uri, timestamp)

    fun getCachedUri(key: String): Uri? {
        val (uri, timestamp) = cache[key] ?: return null
        return if (System.currentTimeMillis() - timestamp < cachePeriodMillis) uri else null
    }

    fun cacheUri(key: String, uri: Uri) {
        cache[key] = uri to System.currentTimeMillis()
    }

    fun isUriValid(key: String): Boolean {
        return getCachedUri(key) != null
    }

    fun refetchIfExpired(key: String, fetcher: () -> Uri): Uri {
        return getCachedUri(key) ?: fetcher().also { cacheUri(key, it) }
    }
}