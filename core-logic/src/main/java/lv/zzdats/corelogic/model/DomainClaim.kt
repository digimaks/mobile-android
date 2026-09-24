// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.model

import eu.europa.ec.eudi.wallet.document.ElementIdentifier

sealed class DomainClaim {
    abstract val key: ElementIdentifier
    abstract val displayTitle: String
    abstract val path: ClaimPath

    data class Group(
        override val key: ElementIdentifier,
        override val displayTitle: String,
        override val path: ClaimPath,
        val items: List<DomainClaim>,
    ) : DomainClaim()

    data class Primitive(
        override val key: ElementIdentifier,
        override val displayTitle: String,
        override val path: ClaimPath,
        val value: String,
        val isRequired: Boolean,
        val intentToRetain: Boolean,
    ) : DomainClaim()
}

data class ClaimPath(val value: List<String>) {
    companion object {
        const val PATH_SEPARATOR = ","

        fun toElementIdentifier(itemId: String): String {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(1)
                .first()
        }

        fun toSdJwtVcPath(itemId: String): List<String> {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(1)
        }
    }

    fun toId(docId: String): String =
        (listOf(docId) + value).joinToString(separator = PATH_SEPARATOR)
}