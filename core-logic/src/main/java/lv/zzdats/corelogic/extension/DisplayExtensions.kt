// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.extension

import eu.europa.ec.eudi.openid4vci.CredentialMetadata
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import lv.zzdats.businesslogic.extensions.getLocalizedString
import java.util.Locale

/**
 * Retrieves the localized claim name from a list of claim displays.
 *
 * This function searches through a list of [DocumentMetaData.Claim.Display] objects
 * to find the name that best matches the user's locale. If an exact
 * match is not found, it falls back to the provided [fallback] string.
 *
 * @param userLocale The user's locale to match against.
 * @param fallback The fallback string to use if no match is found.
 * @return The localized claim name as a string, or the [fallback] string if no
 * matching locale is found.
 *
 * @see getLocalizedString
 */

fun CredentialMetadata?.getLocalizedDisplayName(
    userLocale: Locale,
    fallback: String,
): String {
    return this?.display.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}

fun List<IssuerMetadata.Claim.Display>?.getLocalizedClaimName(
    userLocale: Locale,
    fallback: String,
): String {
    return this.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}
/**
 * Retrieves the localized display name from a list of [Display] objects based on the user's locale.
 *
 * This function searches through a list of [Display] objects to find a display name that matches
 * the provided [userLocale]. If a matching locale is found, the corresponding name is returned.
 * If no matching locale is found, the provided [fallback] string is returned.
 *
 * @param userLocale The user's locale to match against.
 * @param fallback The fallback string to use if no match is found.
 * @return The localized display name if found, otherwise the [fallback] string.
 *
 * @see getLocalizedString
 */
fun List<Display>.getLocalizedDisplayName(
    userLocale: Locale,
    fallback: String,
): String {
    return this.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}

fun IssuerMetadata?.getLocalizedDocumentName(
    userLocale: Locale,
    fallback: String,
): String {
    return this?.display.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}