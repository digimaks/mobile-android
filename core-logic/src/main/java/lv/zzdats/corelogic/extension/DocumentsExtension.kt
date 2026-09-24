// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.extension

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import lv.zzdats.businesslogic.extensions.getLocalizedValue
import java.util.Locale

fun Document.localizedIssuerMetadata(locale: Locale): IssuerMetadata.IssuerDisplay? {
    return issuerMetadata?.issuerDisplay.getLocalizedValue(
        userLocale = locale,
        fallback = null,
        localeExtractor = { it.locale },
        valueExtractor = { it }
    )
}

fun Document.localizedDocumentMetadata(locale: Locale): IssuerMetadata.Display? {
    return issuerMetadata?.display.getLocalizedValue(
        userLocale = locale,
        fallback = null,
        localeExtractor = { it.locale },
        valueExtractor = { it }
    )
}