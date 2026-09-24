// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

import java.util.Locale

fun Locale.compareLocaleLanguage(localeToCompare: Locale?): Boolean =
    localeToCompare?.let { this.language == it.language } == true

fun <T> List<T>?.getLocalizedString(
    userLocale: Locale,
    localeExtractor: (T) -> Locale?,
    stringExtractor: (T) -> String?,
    fallback: String,
): String {
    return this.getLocalizedValue(
        userLocale = userLocale,
        localeExtractor = localeExtractor,
        valueExtractor = stringExtractor,
        fallback = fallback,
    ) ?: fallback
}

fun <T, M> List<T>?.getLocalizedValue(
    userLocale: Locale,
    localeExtractor: (T) -> Locale?,
    valueExtractor: (T) -> M?,
    fallback: M?,
): M? {
    return try {
        // Match based on locale
        this?.find { userLocale.compareLocaleLanguage(localeExtractor(it)) }?.let(valueExtractor)
            ?: this?.firstOrNull()?.let(valueExtractor) // If no matches: Use the first available
            ?: fallback // If list is empty, return the fallback
    } catch (_: Exception) {
        fallback // If an exception occurs, return the fallback
    }
}